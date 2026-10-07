package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** PayHere hosted checkout: a browser completion callback never settles an invoice. */
@Service
public class CashierCardService {
    public record BillingDetails(String firstName,String lastName,String email,String phone,String address,String city,String country) {}
    public record CardRequest(CashierPOSService.Checkout checkout,BillingDetails billing) {}
    private final CashierPOSService pos;
    private final CashierSaleRepository sales;
    private final InvoiceRepository invoices;
    private final SparePartRepository parts;
    private final JobPartUsageRepository usages;
    private final BillingService billing;
    private final NotificationService notifications;
    private final Clock clock;
    private final TransactionTemplate tx;
    @Value("${app.payhere.merchant-id:}") private String merchant;
    @Value("${app.payhere.merchant-secret:}") private String secret;
    @Value("${app.payhere.notify-url:}") private String notifyUrl;
    @Value("${app.payhere.sandbox:true}") private boolean sandbox;
    @Value("${app.frontend.url:http://localhost:5173}") private String frontend;
    public CashierCardService(CashierPOSService pos,CashierSaleRepository sales,InvoiceRepository invoices,SparePartRepository parts,JobPartUsageRepository usages,BillingService billing,NotificationService notifications,Clock clock,PlatformTransactionManager manager){this.pos=pos;this.sales=sales;this.invoices=invoices;this.parts=parts;this.usages=usages;this.billing=billing;this.notifications=notifications;this.clock=clock;this.tx=new TransactionTemplate(manager);}
    private boolean configured(){try{URI uri=URI.create(notifyUrl);return !merchant.isBlank()&&!secret.isBlank()&&"https".equals(uri.getScheme())&&uri.getHost()!=null&&!Set.of("localhost","127.0.0.1","::1").contains(uri.getHost());}catch(Exception e){return false;}}
    public Object configuration(){return Map.of("configured",configured(),"sandbox",sandbox,"provider","PayHere");}
    private String field(String value,String name,int max){String text=Rules.required(value,name);if(text.length()>max||text.chars().anyMatch(c->Character.isISOControl(c)||c=='<'||c=='>'))throw new IllegalArgumentException(name+" contains invalid characters or is too long");return text;}
    private BillingDetails payer(BillingDetails p){if(p==null)throw new IllegalArgumentException("Card billing details are required");String first=field(p.firstName(),"First name",60),last=field(p.lastName(),"Last name",60),email=field(p.email(),"Email",120),phone=field(p.phone(),"Phone",20),address=field(p.address(),"Address",200),city=field(p.city(),"City",80),country=field(p.country(),"Country",80);com.fuelstation.util.InputValidation.name(first,"First name",true);com.fuelstation.util.InputValidation.name(last,"Last name",true);com.fuelstation.util.InputValidation.email(email,true);com.fuelstation.util.InputValidation.phone(phone,"Phone",true);phone=phone.replaceAll("[ ()-]","");return new BillingDetails(first,last,email,phone,address,city,country);}
    public Object start(CardRequest request,String key,String cashier){
        if(!configured())throw new IllegalStateException("Card checkout requires PayHere merchant settings and a public HTTPS notification URL. Cash and QR remain available.");
        if(request==null||request.checkout()==null||!"CARD".equals(request.checkout().paymentMethod()))throw new IllegalArgumentException("A card checkout is required");
        BillingDetails payer=payer(request.billing());
        if(request.checkout().amount()==null||Rules.positive(request.checkout().amount(),"Card payment")<=0)throw new IllegalArgumentException("Card payment must be positive");
        return tx.execute(status->{CashierSale sale=pos.prepare(request.checkout(),key,cashier);if("PREPARED".equals(sale.getState())){sale.setState("PENDING");sale.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));sales.saveAndFlush(sale);}if(!"PENDING".equals(sale.getState()))return pos.receipt(sale);
            Map<String,Object> payment=new LinkedHashMap<>();String amount=String.format(Locale.ROOT,"%.2f",sale.getPaymentAmount());payment.put("sandbox",sandbox);payment.put("merchant_id",merchant);payment.put("return_url",frontend+"/dashboard/pos");payment.put("cancel_url",frontend+"/dashboard/pos");payment.put("notify_url",notifyUrl);payment.put("order_id",sale.getId());payment.put("items","FuelCore invoice "+invoices.findById(sale.getInvoiceId()).orElseThrow().getInvoiceNumber());payment.put("currency","LKR");payment.put("amount",amount);payment.put("hash",md5(merchant+sale.getId()+amount+"LKR"+md5(secret)));payment.put("first_name",payer.firstName());payment.put("last_name",payer.lastName());payment.put("email",payer.email());payment.put("phone",payer.phone());payment.put("address",payer.address());payment.put("city",payer.city());payment.put("country",payer.country());return Map.of("saleId",sale.getId(),"state",sale.getState(),"payment",payment);});
    }
    public Object status(String id,String cashier){return tx.execute(ignored->{CashierSale sale=sales.lockById(id).orElseThrow(()->new IllegalArgumentException("Card checkout not found"));if(!cashier.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another cashier");if("PENDING".equals(sale.getState())&&sale.getExpiresAt().isBefore(LocalDateTime.now(clock))){review(sale,"Gateway confirmation is delayed. Verify this payment with PayHere before taking another payment.");sale.setSettlementBlocked(false);sales.save(sale);}return pos.receipt(sale);});}
    public Object cancelUnprocessed(String id,String cashier){return tx.execute(ignored->{CashierSale sale=sales.lockById(id).orElseThrow(()->new IllegalArgumentException("Card checkout not found"));if(!cashier.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another cashier");if("PENDING".equals(sale.getState())&&sale.getGatewayPaymentId()==null){release(sale);sale.setState("CANCELLED");sale.setMessage("Gateway checkout was closed before processing. Any later gateway collection will require reconciliation.");sales.save(sale);}return pos.receipt(sale);});}
    private void review(CashierSale sale,String message){sale.setState("REVIEW");sale.setSettlementBlocked(true);sale.setMessage(message);sales.save(sale);notifications.notifyRoles("Card payment needs verification","Checkout "+sale.getId()+": "+message,"Billing","/dashboard/payments","MANAGER","CASHIER");}
    public void notify(Map<String,String> input){
        if(!configured())throw new IllegalArgumentException("Card gateway is not configured");
        String order=field(input.get("order_id"),"Order",100),amount=field(input.get("payhere_amount"),"Amount",30),currency=field(input.get("payhere_currency"),"Currency",3),code=field(input.get("status_code"),"Status",4),sig=field(input.get("md5sig"),"Signature",32);
        String expected=md5(merchant+order+amount+currency+code+md5(secret));
        if(!merchant.equals(input.get("merchant_id"))||!"LKR".equals(currency)||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),sig.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))throw new IllegalArgumentException("Invalid gateway signature");
        final double paid;try{paid=Double.parseDouble(amount);}catch(Exception e){throw new IllegalArgumentException("Invalid gateway amount");}if(!Double.isFinite(paid)||paid<=0)throw new IllegalArgumentException("Invalid gateway amount");
        tx.executeWithoutResult(ignored->{CashierSale sale=sales.findById(order).orElseThrow(()->new IllegalArgumentException("Unknown checkout"));if(!"CARD".equals(sale.getPaymentMethod())||Math.abs(sale.getPaymentAmount()-paid)>.001)throw new IllegalArgumentException("Gateway amount differs from card checkout");});
        if("2".equals(code))field(input.get("payment_id"),"Gateway payment ID",100);
        try{tx.executeWithoutResult(ignored->{CashierSale sale=sales.lockById(order).orElseThrow();if("2".equals(code)){String paymentId=input.get("payment_id");if(Boolean.TRUE.equals(sale.getSettlementBlocked())){if(sale.getGatewayPaymentId()==null){sale.setGatewayPaymentId(paymentId);sales.save(sale);}return;}if("SUCCESS".equals(sale.getState())){if(!paymentId.equals(sale.getGatewayPaymentId()))review(sale,"An additional gateway payment arrived for a settled checkout. Reconcile or refund it through PayHere.");return;}if(!Set.of("PENDING","REVIEW").contains(sale.getState())){sale.setGatewayPaymentId(paymentId);review(sale,"A payment arrived after cancellation. Reconcile or refund through PayHere.");return;}if(sale.getGatewayPaymentId()!=null&&!paymentId.equals(sale.getGatewayPaymentId())){review(sale,"Multiple gateway payments were reported. Reconcile with PayHere before settling.");return;}sale.setGatewayPaymentId(paymentId);pos.settle(sale);sale.setMessage(null);sales.save(sale);}else if(Set.of("-1","-2").contains(code)&&Set.of("PENDING","REVIEW").contains(sale.getState())&&sale.getGatewayPaymentId()==null){release(sale);sale.setState("-1".equals(code)?"CANCELLED":"FAILED");sale.setMessage("The payment gateway did not collect this card payment.");sales.save(sale);}else if("-3".equals(code))review(sale,"PayHere reported a chargeback. Reconcile the payment ledger with the provider.");});}
        catch(RuntimeException e){tx.executeWithoutResult(ignored->{CashierSale sale=sales.lockById(order).orElseThrow();if("2".equals(code)&&sale.getGatewayPaymentId()==null)sale.setGatewayPaymentId(input.get("payment_id"));review(sale,"Gateway notification received, but the invoice changed during checkout. Reconcile before taking another payment.");});}
    }
    private void release(CashierSale sale){Invoice inv=invoices.findById(sale.getInvoiceId()).orElseThrow();invoices.lockByInvoiceNumber(inv.getInvoiceNumber()).orElseThrow();if(Math.abs(Rules.amount(inv.getAmountPaid())-sale.getInvoicePaidBefore())>.001||inv.getSettledTotal()!=null)throw new IllegalStateException("Invoice changed while checkout was pending");for(CashierSaleLine line:sale.getLines()){SparePart part=parts.lockById(line.getPartId()).orElseThrow();part.setStockQuantity(part.getStockQuantity()+line.getQuantity());if(!Set.of("Inactive","Deactivated").contains(part.getStatus()==null?"":part.getStatus()))part.setStatus(part.getStockQuantity()<=(part.getMinStockWarning()==null?0:part.getMinStockWarning())?"Low Stock":"Available");parts.save(part);if(line.getUsageId()!=null)usages.deleteById(line.getUsageId());}usages.flush();if(sale.getReferenceNumber()==null){inv.setStatus("VOID");invoices.save(inv);}else billing.updateServiceInvoice(billingBooking(sale));}
    private ServiceBooking billingBooking(CashierSale sale){return pos.quote(new CashierPOSService.Checkout(sale.getReferenceNumber(),List.of(),null,null,null,null,null)).booking();}
    private String md5(String value){try{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
