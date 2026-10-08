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
    public record CardRequest(CashierPOSService.Checkout checkout) {}
    public record InvoiceRequest(String invoiceNumber,Double amount) {}
    public record StoreRequest(Long partId,Integer quantity) {}
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
    @Value("${app.payhere.demo-enabled:true}") private boolean demoEnabled;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontend;
    @Value("${app.payhere.guest-email:}") private String guestEmail;
    @Value("${app.payhere.guest-phone:}") private String guestPhone;
    @Value("${app.payhere.guest-address:}") private String guestAddress;
    @Value("${app.payhere.guest-city:}") private String guestCity;
    @Value("${app.payhere.guest-country:Sri Lanka}") private String guestCountry;
    public CashierCardService(CashierPOSService pos,CashierSaleRepository sales,InvoiceRepository invoices,SparePartRepository parts,JobPartUsageRepository usages,BillingService billing,NotificationService notifications,Clock clock,PlatformTransactionManager manager){this.pos=pos;this.sales=sales;this.invoices=invoices;this.parts=parts;this.usages=usages;this.billing=billing;this.notifications=notifications;this.clock=clock;this.tx=new TransactionTemplate(manager);}
    private boolean configured(){try{URI uri=URI.create(notifyUrl);return !merchant.isBlank()&&!secret.isBlank()&&"https".equals(uri.getScheme())&&uri.getHost()!=null&&!Set.of("localhost","127.0.0.1","::1","[::1]").contains(uri.getHost());}catch(Exception e){return false;}}
    private boolean guestConfigured(){try{guest();return true;}catch(RuntimeException e){return false;}}
    private boolean demoAllowed(){try{return demoEnabled&&sandbox&&Set.of("localhost","127.0.0.1","::1","[::1]").contains(URI.create(frontend).getHost());}catch(RuntimeException e){return false;}}
    private boolean demoMode(){return !(configured()&&guestConfigured())&&demoAllowed();}
    public Object configuration(){boolean real=configured()&&guestConfigured();return Map.of("configured",real||demoAllowed(),"sandbox",sandbox,"provider",real?"PayHere":"Demo","mode",real?"PAYHERE":demoAllowed()?"DEMO":"UNAVAILABLE");}
    private BillingDetails guest(){return payer(new BillingDetails("Guest","Customer",guestEmail,guestPhone,guestAddress,guestCity,guestCountry));}
    private BillingDetails ensureReady(){if(!configured())throw new IllegalStateException("Card checkout requires PayHere merchant settings and a public HTTPS notification URL. Cash and QR remain available.");try{return guest();}catch(RuntimeException e){throw new IllegalStateException("Configure the station guest email, phone, address and city for anonymous PayHere checkout.");}}
    private String field(String value,String name,int max){String text=Rules.required(value,name);if(text.length()>max||text.chars().anyMatch(c->Character.isISOControl(c)||c=='<'||c=='>'))throw new IllegalArgumentException(name+" contains invalid characters or is too long");return text;}
    private BillingDetails payer(BillingDetails p){if(p==null)throw new IllegalArgumentException("Card billing details are required");String first=field(p.firstName(),"First name",60),last=field(p.lastName(),"Last name",60),email=field(p.email(),"Email",254),phone=field(p.phone(),"Phone",25),address=field(p.address(),"Address",200),city=field(p.city(),"City",80),country=field(p.country(),"Country",80);com.fuelstation.util.InputValidation.name(first,"First name",true);com.fuelstation.util.InputValidation.name(last,"Last name",true);com.fuelstation.util.InputValidation.email(email,true);phone=com.fuelstation.util.InputValidation.phone(phone,"Phone",true);address=com.fuelstation.util.InputValidation.address(address,"Address",200,true);return new BillingDetails(first,last,email,phone,address,city,country);}
    public Object start(CardRequest request,String key,String cashier){
        if(demoMode())return startDemoPos(request,key,cashier);
        BillingDetails payer=ensureReady();
        if(request==null||request.checkout()==null||!"CARD".equals(request.checkout().paymentMethod()))throw new IllegalArgumentException("A card checkout is required");
        if(request.checkout().amount()==null||Rules.positive(request.checkout().amount(),"Card payment")<=0)throw new IllegalArgumentException("Card payment must be positive");
        return tx.execute(status->{CashierSale sale=pos.prepare(request.checkout(),key,cashier);if("PREPARED".equals(sale.getState())){sale.setState("PENDING");sale.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));sales.saveAndFlush(sale);}if(!"PENDING".equals(sale.getState()))return pos.receipt(sale);
            return checkoutPayload(sale,payer,"/dashboard/pos");});
    }
    private Object checkoutPayload(CashierSale sale,BillingDetails payer,String route){
        if(!"PENDING".equals(sale.getState()))return pos.receipt(sale);
        Map<String,Object> payment=new LinkedHashMap<>();String amount=String.format(Locale.ROOT,"%.2f",sale.getPaymentAmount());
        payment.put("sandbox",sandbox);payment.put("merchant_id",merchant);payment.put("return_url",frontend+route);payment.put("cancel_url",frontend+route);payment.put("notify_url",notifyUrl);payment.put("order_id",sale.getId());payment.put("items","FuelCore invoice "+invoices.findById(sale.getInvoiceId()).orElseThrow().getInvoiceNumber());payment.put("currency","LKR");payment.put("amount",amount);payment.put("hash",md5(merchant+sale.getId()+amount+"LKR"+md5(secret)));
        payment.put("first_name",payer.firstName());payment.put("last_name",payer.lastName());payment.put("email",payer.email());payment.put("phone",payer.phone());payment.put("address",payer.address());payment.put("city",payer.city());payment.put("country",payer.country());return Map.of("saleId",sale.getId(),"state",sale.getState(),"payment",payment);
    }
    public Object startInvoice(InvoiceRequest request,String key,String username,boolean customer){
        boolean demo=demoMode();BillingDetails payer=demo?null:ensureReady();
        if(request==null)throw new IllegalArgumentException("Invoice payment is required");
        String number=Rules.required(request.invoiceNumber(),"Invoice number");double amount=Rules.positive(request.amount(),"Card payment");
        if(key==null||!key.matches("[A-Za-z0-9-]{8,100}"))throw new IllegalArgumentException("A valid checkout request key is required");
        return tx.execute(status->{
            Invoice inv=invoices.lockByInvoiceNumber(number).orElseThrow(()->new IllegalArgumentException("Invoice not found"));
            if(customer&&!username.equals(inv.getCustomerUsername()))throw new org.springframework.security.access.AccessDeniedException("Invoice is not yours");
            CashierSale prior=sales.findById(key).orElse(null);
            if(prior!=null){if(!Set.of("INVOICE","DEMO_INVOICE").contains(prior.getCheckoutType())||!username.equals(prior.getRecordedBy())||!inv.getId().equals(prior.getInvoiceId())||Math.abs(prior.getPaymentAmount()-amount)>.001)throw new IllegalArgumentException("Checkout request key was used for different details");return isDemo(prior)?demoPayload(prior):checkoutPayload(prior,payer,customer?"/customer/invoices":"/dashboard/invoices");}
            double balance=inv.getBalanceDue();
            if(balance<=.001||Set.of("VOID","REFUNDED","PARTIALLY_REFUNDED").contains(inv.getStatus())||amount>balance+.001)throw new IllegalArgumentException("Payment must be within the outstanding invoice balance");
            if("SERVICE".equals(inv.getInvoiceType())&&!Boolean.TRUE.equals(inv.getFinalized())&&amount>=balance-.001)throw new IllegalArgumentException("Complete the service before full settlement");
            if("SPARE_PART".equals(inv.getInvoiceType())&&amount<balance-.001)throw new IllegalArgumentException("Spare parts require full payment");
            if(sales.findByInvoiceId(inv.getId()).stream().anyMatch(s->Set.of("PENDING","REVIEW").contains(s.getState())))throw new IllegalArgumentException("A card checkout for this invoice is pending or needs verification");
            CashierSale sale=new CashierSale();sale.setId(key);sale.setCheckoutType("INVOICE");sale.setRecordedBy(username);sale.setRecordedAt(LocalDateTime.now(clock));sale.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));sale.setReferenceNumber(inv.getReferenceNumber());sale.setInvoiceId(inv.getId());sale.setPaymentMethod("CARD");sale.setPaymentAmount(amount);sale.setInvoicePaidBefore(Rules.amount(inv.getAmountPaid()));sale.setOfferSavings(0.0);sale.setChangeDue(0.0);sale.setState("PENDING");sales.saveAndFlush(sale);
            if(demo){sale.setCheckoutType("DEMO_INVOICE");sale.setState("DEMO_PENDING");sale.setDemoInvoiceTotal(inv.getNetTotalWithPenalty());sale.setDemoInvoiceBalance(balance);sales.saveAndFlush(sale);return demoPayload(sale);}
            return checkoutPayload(sale,payer,customer?"/customer/invoices":"/dashboard/invoices");
        });
    }
    public Object startStore(StoreRequest request,String key,String username){
        if(demoMode())return startDemoStore(request,key,username);
        BillingDetails payer=ensureReady();if(key==null||!key.matches("[A-Za-z0-9-]{8,100}"))throw new IllegalArgumentException("A valid checkout request key is required");if(request==null||request.partId()==null)throw new IllegalArgumentException("Choose a spare part");int quantity=Rules.quantity(request.quantity(),"Quantity");
        return tx.execute(status->{
            CashierSale prior=sales.findById(key).orElse(null);
            if(prior!=null){if(!username.equals(prior.getRecordedBy())||!"STORE".equals(prior.getCheckoutType())||prior.getLines().size()!=1||!request.partId().equals(prior.getLines().get(0).getPartId())||quantity!=prior.getLines().get(0).getQuantity())throw new IllegalArgumentException("Checkout request key was used for different details");return checkoutPayload(prior,payer,"/customer/store");}
            var items=List.of(new CashierPOSService.CartItem(request.partId(),quantity));var quote=pos.quote(new CashierPOSService.Checkout(null,items,"CARD",null,null,null,null));
            if(quote.balance()<=0)throw new IllegalArgumentException("Use Cash for a free parts purchase");
            CashierSale sale=pos.prepare(new CashierPOSService.Checkout(null,items,"CARD",quote.balance(),null,quote.quoteToken(),false),key,username);
            Invoice inv=invoices.findById(sale.getInvoiceId()).orElseThrow();inv.setCustomerUsername(username);inv.setCustomerName(username);inv.setCustomerType("REGISTERED_CUSTOMER");invoices.save(inv);
            sale.setCheckoutType("STORE");sale.setState("PENDING");sale.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));sales.saveAndFlush(sale);return checkoutPayload(sale,payer,"/customer/store");
        });
    }
    public Object status(String id,String cashier){return tx.execute(ignored->{CashierSale sale=sales.lockById(id).orElseThrow(()->new IllegalArgumentException("Card checkout not found"));if(!cashier.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another cashier");if(isDemo(sale))return demoReceipt(sale);if("PENDING".equals(sale.getState())&&sale.getExpiresAt().isBefore(LocalDateTime.now(clock))){review(sale,"Gateway confirmation is delayed. Verify this payment with PayHere before taking another payment.");sale.setSettlementBlocked(false);sales.save(sale);}return pos.receipt(sale);});}
    public Object cancelUnprocessed(String id,String cashier){return tx.execute(ignored->{CashierSale sale=sales.lockById(id).orElseThrow(()->new IllegalArgumentException("Card checkout not found"));if(!cashier.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another cashier");if(isDemo(sale)){if("DEMO_PENDING".equals(sale.getState())){sale.setState("CANCELLED");sales.save(sale);}return demoReceipt(sale);}if("PENDING".equals(sale.getState())&&sale.getGatewayPaymentId()==null){release(sale);sale.setState("CANCELLED");sale.setMessage("Gateway checkout was closed before processing. Any later gateway collection will require reconciliation.");sales.save(sale);}return pos.receipt(sale);});}
    private void review(CashierSale sale,String message){sale.setState("REVIEW");sale.setSettlementBlocked(true);sale.setMessage(message);sales.save(sale);notifications.notifyRoles("Card payment needs verification","Checkout "+sale.getId()+": "+message,"Billing","/dashboard/payments","MANAGER","CASHIER");}
    public void notify(Map<String,String> input){
        if(!configured())throw new IllegalArgumentException("Card gateway is not configured");
        String order=field(input.get("order_id"),"Order",100),amount=field(input.get("payhere_amount"),"Amount",30),currency=field(input.get("payhere_currency"),"Currency",3),code=field(input.get("status_code"),"Status",4),sig=field(input.get("md5sig"),"Signature",32);
        String expected=md5(merchant+order+amount+currency+code+md5(secret));
        if(!merchant.equals(input.get("merchant_id"))||!"LKR".equals(currency)||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),sig.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))throw new IllegalArgumentException("Invalid gateway signature");
        final double paid;try{paid=Double.parseDouble(amount);}catch(Exception e){throw new IllegalArgumentException("Invalid gateway amount");}if(!Double.isFinite(paid)||paid<=0)throw new IllegalArgumentException("Invalid gateway amount");
        tx.executeWithoutResult(ignored->{CashierSale sale=sales.findById(order).orElseThrow(()->new IllegalArgumentException("Unknown checkout"));if(isDemo(sale))throw new IllegalArgumentException("Demo checkout cannot receive a real gateway payment");if(!"CARD".equals(sale.getPaymentMethod())||Math.abs(sale.getPaymentAmount()-paid)>.001)throw new IllegalArgumentException("Gateway amount differs from card checkout");});
        if("2".equals(code))field(input.get("payment_id"),"Gateway payment ID",100);
        try{tx.executeWithoutResult(ignored->{CashierSale sale=sales.lockById(order).orElseThrow();if("2".equals(code)){String paymentId=input.get("payment_id");if(Boolean.TRUE.equals(sale.getSettlementBlocked())){if(sale.getGatewayPaymentId()==null){sale.setGatewayPaymentId(paymentId);sales.save(sale);}return;}if("SUCCESS".equals(sale.getState())){if(!paymentId.equals(sale.getGatewayPaymentId()))review(sale,"An additional gateway payment arrived for a settled checkout. Reconcile or refund it through PayHere.");return;}if(!Set.of("PENDING","REVIEW").contains(sale.getState())){sale.setGatewayPaymentId(paymentId);review(sale,"A payment arrived after cancellation. Reconcile or refund through PayHere.");return;}if(sale.getGatewayPaymentId()!=null&&!paymentId.equals(sale.getGatewayPaymentId())){review(sale,"Multiple gateway payments were reported. Reconcile with PayHere before settling.");return;}sale.setGatewayPaymentId(paymentId);pos.settle(sale);sale.setMessage(null);sales.save(sale);}else if(Set.of("-1","-2").contains(code)&&Set.of("PENDING","REVIEW").contains(sale.getState())&&sale.getGatewayPaymentId()==null){release(sale);sale.setState("-1".equals(code)?"CANCELLED":"FAILED");sale.setMessage("The payment gateway did not collect this card payment.");sales.save(sale);}else if("-3".equals(code))review(sale,"PayHere reported a chargeback. Reconcile the payment ledger with the provider.");});}
        catch(RuntimeException e){tx.executeWithoutResult(ignored->{CashierSale sale=sales.lockById(order).orElseThrow();if("2".equals(code)&&sale.getGatewayPaymentId()==null)sale.setGatewayPaymentId(input.get("payment_id"));review(sale,"Gateway notification received, but the invoice changed during checkout. Reconcile before taking another payment.");});}
    }
    private void release(CashierSale sale){if("INVOICE".equals(sale.getCheckoutType()))return;Invoice inv=invoices.findById(sale.getInvoiceId()).orElseThrow();invoices.lockByInvoiceNumber(inv.getInvoiceNumber()).orElseThrow();if(Math.abs(Rules.amount(inv.getAmountPaid())-sale.getInvoicePaidBefore())>.001||inv.getSettledTotal()!=null)throw new IllegalStateException("Invoice changed while checkout was pending");for(CashierSaleLine line:sale.getLines()){SparePart part=parts.lockById(line.getPartId()).orElseThrow();part.setStockQuantity(part.getStockQuantity()+line.getQuantity());if(!Set.of("Inactive","Deactivated").contains(part.getStatus()==null?"":part.getStatus()))part.setStatus(part.getStockQuantity()<=(part.getMinStockWarning()==null?0:part.getMinStockWarning())?"Low Stock":"Available");parts.save(part);if(line.getUsageId()!=null)usages.deleteById(line.getUsageId());}usages.flush();if(sale.getReferenceNumber()==null){inv.setStatus("VOID");invoices.save(inv);}else billing.updateServiceInvoice(billingBooking(sale));}
    private boolean isDemo(CashierSale sale){return sale.getCheckoutType()!=null&&sale.getCheckoutType().startsWith("DEMO_");}
    private String demoKey(String key){if(key==null||!key.matches("[A-Za-z0-9-]{8,100}"))throw new IllegalArgumentException("A valid checkout request key is required");return key;}
    private Object startDemoPos(CardRequest request,String key,String username){
        demoKey(key);if(request==null||request.checkout()==null||!"CARD".equals(request.checkout().paymentMethod()))throw new IllegalArgumentException("Choose a card checkout");
        var input=request.checkout();String fingerprint=md5(input.toString());
        return tx.execute(status->{
            CashierSale prior=sales.findById(key).orElse(null);
            if(prior!=null){if(!"DEMO_POS".equals(prior.getCheckoutType())||!username.equals(prior.getRecordedBy())||!fingerprint.equals(prior.getRequestFingerprint()))throw new IllegalArgumentException("Checkout request key was used for different details");return demoPayload(prior);}
            var quote=pos.quote(input);double amount=Rules.positive(input.amount(),"Card payment");
            if(input.quoteToken()==null||!input.quoteToken().equals(quote.quoteToken()))throw new IllegalArgumentException("Prices or invoice details changed. Refresh the receipt before confirming");
            if(amount>quote.balance()+.001)throw new IllegalArgumentException("Payment exceeds the invoice balance");
            if(quote.referenceNumber()==null&&Math.abs(amount-quote.balance())>.001)throw new IllegalArgumentException("Spare parts require full payment");
            if(!quote.finalized()&&amount>=quote.balance()-.001)throw new IllegalArgumentException("Complete the service before full settlement");
            CashierSale sale=new CashierSale();sale.setId(key);sale.setCheckoutType("DEMO_POS");sale.setRequestFingerprint(fingerprint);sale.setRecordedBy(username);sale.setRecordedAt(LocalDateTime.now(clock));sale.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));sale.setReferenceNumber(quote.referenceNumber());sale.setInvoiceId(quote.invoice()==null?null:quote.invoice().getId());sale.setPaymentMethod("CARD");sale.setPaymentAmount(amount);sale.setInvoicePaidBefore(quote.paid());sale.setDemoInvoiceTotal(quote.total());sale.setDemoInvoiceBalance(quote.balance());sale.setOfferSavings(quote.offerSavings());sale.setChangeDue(0.0);sale.setState("DEMO_PENDING");
            for(var row:quote.lines()){CashierSaleLine line=new CashierSaleLine();line.setPartId(row.partId());line.setItemName(row.itemName());line.setQuantity(row.quantity());line.setUnitPrice(row.unitPrice());line.setOriginalUnitPrice(row.originalUnitPrice());sale.getLines().add(line);}
            sales.saveAndFlush(sale);return demoPayload(sale);
        });
    }
    private Object startDemoStore(StoreRequest request,String key,String username){
        demoKey(key);if(request==null||request.partId()==null)throw new IllegalArgumentException("Choose a spare part");int quantity=Rules.quantity(request.quantity(),"Quantity");
        return tx.execute(status->{
            CashierSale prior=sales.findById(key).orElse(null);
            if(prior!=null){if(!"DEMO_STORE".equals(prior.getCheckoutType())||!username.equals(prior.getRecordedBy())||prior.getLines().size()!=1||!request.partId().equals(prior.getLines().get(0).getPartId())||quantity!=prior.getLines().get(0).getQuantity())throw new IllegalArgumentException("Checkout request key was used for different details");return demoPayload(prior);}
            var items=List.of(new CashierPOSService.CartItem(request.partId(),quantity));var quote=pos.quote(new CashierPOSService.Checkout(null,items,"CARD",null,null,null,null));
            startDemoPos(new CardRequest(new CashierPOSService.Checkout(null,items,"CARD",quote.balance(),null,quote.quoteToken(),false)),key,username);
            CashierSale sale=sales.findById(key).orElseThrow();sale.setCheckoutType("DEMO_STORE");sales.saveAndFlush(sale);return demoPayload(sale);
        });
    }
    private Object demoPayload(CashierSale sale){if(!"DEMO_PENDING".equals(sale.getState()))return demoReceipt(sale);return Map.of("saleId",sale.getId(),"state",sale.getState(),"demo",true,"demoCheckout",true,"amount",sale.getPaymentAmount());}
    private Object demoReceipt(CashierSale sale){
        Invoice original=sale.getInvoiceId()==null?null:invoices.findById(sale.getInvoiceId()).orElseThrow();
        boolean completed="DEMO_COMPLETED".equals(sale.getState());double paid=Rules.amount(sale.getInvoicePaidBefore())+(completed?sale.getPaymentAmount():0),balance=Math.max(0,Rules.amount(sale.getDemoInvoiceBalance())-(completed?sale.getPaymentAmount():0));
        Map<String,Object> invoice=new LinkedHashMap<>();invoice.put("id",original==null?null:original.getId());invoice.put("invoiceNumber",original==null?"DEMO-"+sale.getId().substring(0,8).toUpperCase(Locale.ROOT):original.getInvoiceNumber());invoice.put("invoiceType",original==null?"SPARE_PART":original.getInvoiceType());invoice.put("referenceNumber",sale.getReferenceNumber());invoice.put("customerName",original==null?"Guest Customer":original.getCustomerName());invoice.put("invoiceDate",sale.getRecordedAt().toLocalDate());invoice.put("paymentMethod","CARD");invoice.put("netTotal",sale.getDemoInvoiceTotal());invoice.put("netTotalWithPenalty",sale.getDemoInvoiceTotal());invoice.put("amountPaid",Rules.money(paid));invoice.put("balanceDue",Rules.money(balance));invoice.put("status",completed?(balance<=.001?"PAID":"PARTIAL"):"PENDING");
        if(original!=null)invoice.put("lineItems",original.getLineItems());else invoice.put("lineItems",sale.getLines().stream().map(line->Map.of("itemName",line.getItemName(),"quantity",line.getQuantity(),"unitPrice",line.getUnitPrice(),"totalAmount",Rules.money(line.getUnitPrice()*line.getQuantity()))).toList());
        Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("completed",completed);result.put("saleId",sale.getId());result.put("state",sale.getState());result.put("demo",true);result.put("invoice",invoice);result.put("paymentAmount",sale.getPaymentAmount());result.put("paymentMethod","CARD");result.put("paymentDate",sale.getRecordedAt().toLocalDate());result.put("changeDue",0.0);result.put("offerSavings",sale.getOfferSavings());result.put("message","Demo receipt only. No money was collected; invoice payments and stock are unchanged.");return result;
    }
    public Object completeDemo(String id,String username){
        if(!demoAllowed())throw new IllegalArgumentException("Demo payments are available only in the local sandbox");
        return tx.execute(status->{CashierSale sale=sales.lockById(id).orElseThrow(()->new IllegalArgumentException("Checkout not found"));if(!username.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another user");if(!isDemo(sale))throw new IllegalArgumentException("Real card payments require a signed PayHere notification");if("DEMO_PENDING".equals(sale.getState())){sale.setState("DEMO_COMPLETED");sale.setCompletedAt(LocalDateTime.now(clock));sales.saveAndFlush(sale);}return demoReceipt(sale);});
    }
    private ServiceBooking billingBooking(CashierSale sale){return pos.quote(new CashierPOSService.Checkout(sale.getReferenceNumber(),List.of(),null,null,null,null,null)).booking();}
    private String md5(String value){try{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
