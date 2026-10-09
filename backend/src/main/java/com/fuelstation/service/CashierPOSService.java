package com.fuelstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class CashierPOSService {
    public record CartItem(Long partId,Integer quantity) {}
    public record Checkout(String referenceNumber,List<CartItem> items,String paymentMethod,
                           Double amount,Double cashTendered,String quoteToken,Boolean qrConfirmed) {}
    public record QuoteLine(Long partId,String itemName,int quantity,double originalUnitPrice,
                            double unitPrice,double totalAmount,boolean existing,String imageUrl) {}
    public record Quote(String referenceNumber,ServiceBooking booking,Invoice invoice,String customerType,
                        List<QuoteLine> lines,double subtotal,double offerSavings,double recordedDiscount,
                        double charges,double total,double paid,double balance,boolean finalized,
                        LocalDate issueDate,LocalDate dueDate,String quoteToken) {}

    @Autowired private BillingService billing;
    @Autowired private BookingService bookings;
    @Autowired private ServiceCatalogService catalog;
    @Autowired private OfferService offers;
    @Autowired private SparePartRepository parts;
    @Autowired private InvoiceRepository invoices;
    @Autowired private JobPartUsageRepository usages;
    @Autowired private CashierSaleRepository sales;
    @Autowired private NotificationService notifications;
    @Autowired private ObjectMapper json;
    @Autowired private Clock clock;

    private String reference(String input) {
        if(input==null||input.isBlank())return null;
        String ref=input.trim().toUpperCase(Locale.ROOT);
        if(!ref.matches("[A-Z0-9-]{3,64}"))throw new IllegalArgumentException("Enter a valid service reference number");
        return ref;
    }
    private List<CartItem> cart(List<CartItem> input) {
        if(input==null)return List.of();
        if(input.size()>100)throw new IllegalArgumentException("Use at most 100 different parts per invoice");
        Set<Long> seen=new HashSet<>();
        for(CartItem row:input){
            if(row==null||row.partId()==null||row.partId()<1||!seen.add(row.partId()))throw new IllegalArgumentException("Select each valid part once; adjust its quantity instead");
            if(Rules.quantity(row.quantity(),"Quantity")>9999)throw new IllegalArgumentException("Quantity must not exceed 9999");
        }
        return input.stream().sorted(Comparator.comparing(CartItem::partId)).toList();
    }
    private double discount(OfferTargetType type,Long id) {
        double value=offers.getActiveOffersByType(type).stream().filter(o->Objects.equals(id,o.getTargetId())).findFirst().map(Offer::getDiscountPercentage).orElse(0.0);
        if(!Double.isFinite(value)||value<0||value>100)throw new IllegalStateException("This item's offer is invalid; ask the Manager to correct it");
        return value;
    }
    private double unit(SparePart part){return Rules.money(Rules.nonnegative(part.getSellingPrice(),"Part price")*(1-discount(OfferTargetType.SPARE_PART,part.getId())/100));}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> products(){
        return billing.getAllSpareParts().stream().map(p->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",p.getId());row.put("partName",p.getPartName());
            row.put("category",p.getCategory()==null?"Essentials":p.getCategory());
            row.put("stockQuantity",p.getStockQuantity()==null?0:p.getStockQuantity());
            row.put("sellingPrice",p.getSellingPrice());row.put("price",unit(p));
            row.put("discountPercentage",discount(OfferTargetType.SPARE_PART,p.getId()));
            row.put("imageUrl",p.getImageUrl());
            row.put("status",p.getStatus());row.put("supplier",p.getSupplier());
            row.put("minStockWarning",p.getMinStockWarning());
            row.put("available",!Set.of("Inactive","Deactivated").contains(p.getStatus()==null?"":p.getStatus()));
            return row;
        }).toList();
    }
    @Transactional(readOnly=true)
    public Object services(){
        return catalog.getAllServices().stream().map(s->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",s.getId());row.put("name",s.getName());row.put("category",s.getCategory());
            row.put("estimatedDuration",s.getEstimatedDuration());row.put("shortDescription",s.getShortDescription());
            row.put("estimatedCost",s.getEstimatedCost());row.put("active",s.isActive());
            row.put("popular",s.isPopular());row.put("icon",s.getIcon());row.put("highlights",s.getHighlights());
            double rate=discount(OfferTargetType.SERVICE,s.getId());
            row.put("discountPercentage",rate);row.put("price",Rules.money(s.getEstimatedCost()*(1-rate/100)));
            row.put("imageUrl",s.getImageUrl());
            return row;
        }).toList();
    }

    @Transactional(readOnly=true)
    public Quote quote(Checkout input){
        if(input==null)throw new IllegalArgumentException("Checkout details are required");
        String ref=reference(input.referenceNumber());List<CartItem> cart=cart(input.items());
        ServiceBooking booking=ref==null?null:bookings.getBookingByReferenceNumber(ref).orElseThrow(()->new IllegalArgumentException("No service matches this reference number"));
        if(booking!=null&&"Cancelled".equals(booking.getStatus()))throw new IllegalArgumentException("This booking was cancelled");
        Invoice inv=ref==null?null:invoices.findByReferenceNumber(ref).orElse(null);
        if(inv!=null&&Set.of("VOID","REFUNDED","PARTIALLY_REFUNDED").contains(inv.getStatus()))throw new IllegalArgumentException("This service invoice is closed");
        if(inv!=null&&inv.getSettledTotal()!=null&&!cart.isEmpty())throw new IllegalArgumentException("This invoice is settled. Sell extra items as a new spare-part sale");
        List<QuoteLine> lines=new ArrayList<>();double base=0,paid=0,charges=0,recordedDiscount=0;
        boolean finalized=booking==null||"Completed".equals(booking.getStatus());
        String type=booking==null?"GUEST_CUSTOMER":bookings.deriveCustomerType(booking);
        if(inv!=null){base=Rules.amount(inv.getGrossTotalAmount());paid=Rules.amount(inv.getAmountPaid());charges=inv.getPreOverdueInterest()+inv.getOverduePenalty();recordedDiscount=Rules.amount(inv.getDiscountAmount());for(InvoiceItem line:inv.getLineItems())lines.add(new QuoteLine(line.getPartId(),line.getItemName(),line.getQuantity(),line.getUnitPrice(),line.getUnitPrice(),line.getTotalAmount(),true,""));}
        else if(booking!=null){base=Rules.nonnegative(booking.getEstimatedCost(),"Service price");lines.add(new QuoteLine(null,booking.getServiceType(),1,base,base,base,true,""));}
        double added=0,savings=0;
        if(booking!=null){ServiceCatalogItem item=catalog.getServiceByName(booking.getServiceType()).orElse(null);if(item!=null){double regular=Rules.nonnegative(item.getEstimatedCost(),"Service price"),rate=discount(OfferTargetType.SERVICE,item.getId()),offered=Rules.money(regular*(1-rate/100)),booked=Rules.amount(booking.getEstimatedCost());boolean matchingOffer=rate>0&&Math.abs(offered-booked)<.001;for(int i=0;i<lines.size();i++){QuoteLine line=lines.get(i);if(line.partId()==null&&line.itemName().equals(booking.getServiceType())){double original=matchingOffer?regular:line.unitPrice();lines.set(i,new QuoteLine(null,line.itemName(),line.quantity(),original,line.unitPrice(),line.totalAmount(),true,item.getImageUrl()));if(matchingOffer)savings+=Rules.money((original-line.unitPrice())*line.quantity());}}}}
        for(CartItem item:cart){SparePart p=parts.findById(item.partId()).orElseThrow(()->new IllegalArgumentException("Part not found"));if(Set.of("Inactive","Deactivated").contains(p.getStatus()==null?"":p.getStatus()))throw new IllegalArgumentException("This part is unavailable");if(p.getStockQuantity()==null||p.getStockQuantity()<item.quantity())throw new IllegalArgumentException(p.getPartName()+": quantity exceeds available stock");double original=Rules.nonnegative(p.getSellingPrice(),"Part price"),price=unit(p),total=Rules.money(price*item.quantity());added+=total;savings+=Rules.money((original-price)*item.quantity());lines.add(new QuoteLine(p.getId(),p.getPartName(),item.quantity(),original,price,total,false,p.getImageUrl()));}
        if(inv!=null&&added>0){Invoice preview=new Invoice();org.springframework.beans.BeanUtils.copyProperties(inv,preview);preview.setGrossTotalAmount(Rules.money(base+added));charges=preview.getPreOverdueInterest()+preview.getOverduePenalty();}
        double subtotal=Rules.money(base+added+savings),total=Rules.money(base+added-recordedDiscount+charges),balance=inv!=null&&inv.getSettledTotal()!=null?0:Rules.money(Math.max(0,total-paid));
        LocalDate issue=inv==null?LocalDate.now(clock):inv.getInvoiceDate(),due=inv==null?null:inv.getDueDate();
        if(booking==null)due=issue;else if(finalized&&due==null){LocalDate start=LocalDate.now(clock);due=type.equals("REGISTERED_CUSTOMER")?start.plusDays(7):type.equals("REGISTERED_VEHICLE")?start.plusDays(3):start;}
        String token=hash(Arrays.asList(ref,cart,lines,inv==null?null:inv.getVersion(),type,LocalDate.now(clock),total,balance));
        return new Quote(ref,booking,inv,type,lines,subtotal,Rules.money(savings),recordedDiscount,Rules.money(charges),total,paid,balance,finalized,issue,due,token);
    }
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value)));}catch(Exception e){throw new IllegalStateException("Cannot verify checkout details",e);}}
    private String requestKey(String input){String key=Rules.required(input,"Checkout request key");if(!key.matches("[A-Za-z0-9-]{8,100}"))throw new IllegalArgumentException("Invalid checkout request key");return key;}
    private String fingerprint(Checkout input){return hash(Arrays.asList(reference(input.referenceNumber()),cart(input.items()),input.paymentMethod(),input.amount(),input.cashTendered(),input.quoteToken(),input.qrConfirmed()));}
    public Object checkout(Checkout input,String key,String cashier){
        if(!Set.of("CASH","QR").contains(input.paymentMethod()==null?"":input.paymentMethod()))throw new IllegalArgumentException("Card payments must be confirmed through the payment gateway");
        CashierSale sale=prepare(input,key,cashier);
        if("SUCCESS".equals(sale.getState()))return receipt(sale);
        settle(sale);
        return receipt(sale);
    }
    CashierSale prepare(Checkout input,String inputKey,String cashier){
        if (input == null || !Set.of("CASH", "CARD", "QR").contains(input.paymentMethod())) throw new IllegalArgumentException("Select Cash, Card or QR");
        String key=requestKey(inputKey),fingerprint=fingerprint(input);
        CashierSale prior=sales.findById(key).orElse(null);
        if(prior!=null){if(!Objects.equals(cashier,prior.getRecordedBy())||!fingerprint.equals(prior.getRequestFingerprint()))throw new IllegalArgumentException("Checkout request key was used for different details");return prior;}
        String ref=reference(input.referenceNumber());
        if(ref!=null){Invoice existing=invoices.findByReferenceNumber(ref).orElse(null);if(existing!=null)invoices.lockByInvoiceNumber(existing.getInvoiceNumber()).orElseThrow();prior=sales.findById(key).orElse(null);if(prior!=null){if(!Objects.equals(cashier,prior.getRecordedBy())||!fingerprint.equals(prior.getRequestFingerprint()))throw new IllegalArgumentException("Checkout request key was used for different details");return prior;}if(sales.findByReferenceNumber(ref).stream().anyMatch(s->Set.of("PENDING","REVIEW").contains(s.getState())))throw new IllegalArgumentException("A card payment for this service is pending or needs verification");}
        List<CartItem> items=cart(input.items());for(CartItem row:items)parts.lockById(row.partId()).orElseThrow(()->new IllegalArgumentException("Part not found"));
        prior=sales.findById(key).orElse(null);if(prior!=null){if(!Objects.equals(cashier,prior.getRecordedBy())||!fingerprint.equals(prior.getRequestFingerprint()))throw new IllegalArgumentException("Checkout request key was used for different details");return prior;}
        Quote quote=quote(input);
        if(input.quoteToken()==null||!MessageDigest.isEqual(input.quoteToken().getBytes(StandardCharsets.UTF_8),quote.quoteToken().getBytes(StandardCharsets.UTF_8)))throw new IllegalArgumentException("Prices or invoice details changed. Refresh the receipt before confirming");
        if(ref==null&&items.isEmpty())throw new IllegalArgumentException("Add at least one spare part");
        double amount=input.amount()==null?quote.balance():Rules.nonnegative(input.amount(),"Payment amount");
        if(ref==null&&Math.abs(amount-quote.balance())>.001)throw new IllegalArgumentException("Spare-part sales require full payment");
        if(amount>quote.balance()+.001)throw new IllegalArgumentException("Payment exceeds the remaining balance");
        if(!quote.finalized()&&amount>=quote.balance()-.001&&quote.balance()>0)throw new IllegalArgumentException("Complete the service before full settlement; record an advance or partial payment");
        if(amount==0&&items.isEmpty())throw new IllegalArgumentException("There is no new sale or payment to record");
        if("CASH".equals(input.paymentMethod())&&Rules.nonnegative(input.cashTendered(),"Cash received")+.001<amount)throw new IllegalArgumentException("Cash received must cover this payment");
        if("QR".equals(input.paymentMethod())&&!Boolean.TRUE.equals(input.qrConfirmed()))throw new IllegalArgumentException("Confirm that the bank QR payment was received");
        CashierSale sale=new CashierSale();sale.setId(key);sale.setRequestFingerprint(fingerprint);sale.setRecordedBy(cashier);sale.setRecordedAt(LocalDateTime.now(clock));sale.setReferenceNumber(ref);sale.setPaymentMethod(input.paymentMethod());sale.setPaymentAmount(amount);sale.setOfferSavings(quote.offerSavings());sale.setChangeDue("CASH".equals(input.paymentMethod())?Rules.money(input.cashTendered()-amount):0.0);sale.setState("PREPARING");sales.saveAndFlush(sale);
        for(QuoteLine line:quote.lines().stream().filter(l->!l.existing()).toList()){
            SparePart part=parts.findById(line.partId()).orElseThrow();part.setStockQuantity(part.getStockQuantity()-line.quantity());part.setStatus(part.getStockQuantity()<=(part.getMinStockWarning()==null?0:part.getMinStockWarning())?"Low Stock":"Available");parts.save(part);
            CashierSaleLine saved=new CashierSaleLine();saved.setPartId(part.getId());saved.setItemName(part.getPartName());saved.setQuantity(line.quantity());saved.setUnitPrice(line.unitPrice());saved.setOriginalUnitPrice(line.originalUnitPrice());
            if(ref!=null){JobPartUsage extra=new JobPartUsage();extra.setReferenceNumber(ref);extra.setPartId(part.getId());extra.setPartName(part.getPartName());extra.setQuantity(line.quantity());extra.setUnitPrice(line.unitPrice());extra.setTotalAmount(line.totalAmount());extra.setRecordedBy(cashier);extra.setRecordedAt(LocalDateTime.now(clock));extra.setRequestKey("pos-"+key+"-"+part.getId());saved.setUsageId(usages.saveAndFlush(extra).getId());}
            sale.getLines().add(saved);
            if("Low Stock".equals(part.getStatus()))notifications.notifyRoles("Spare part stock low",part.getPartName()+" has "+part.getStockQuantity()+" units remaining","Inventory","/dashboard/pos","MANAGER","CASHIER");
        }
        Invoice inv;
        if(ref!=null)inv=billing.updateServiceInvoice(quote.booking());
        else{inv=new Invoice();inv.setInvoiceType("SPARE_PART");inv.setInvoiceName("Counter spare-part sale");inv.setCustomerName("Guest Customer");inv.setCustomerType("GUEST_CUSTOMER");inv.setPurchaseRequestKey(key);inv.setInvoiceDate(LocalDate.now(clock));inv.setDueDate(inv.getInvoiceDate());double total=Rules.money(quote.total());inv.setGrossTotalAmount(total);inv.setNetTotal(total);inv.setConvenienceFee(0.0);inv.setDiscountAmount(0.0);inv.setFinalized(true);inv.setPaymentMethod(input.paymentMethod());inv.setCashTendered(input.cashTendered());for(CashierSaleLine line:sale.getLines()){InvoiceItem item=new InvoiceItem();item.setPartId(line.getPartId());item.setItemName(line.getItemName());item.setQuantity(line.getQuantity());item.setUnitPrice(line.getUnitPrice());item.setTotalAmount(Rules.money(line.getUnitPrice()*line.getQuantity()));inv.getLineItems().add(item);}inv=billing.saveInvoice(inv);}
        sale.setInvoiceId(inv.getId());sale.setInvoicePaidBefore(Rules.amount(inv.getAmountPaid()));sale.setState("PREPARED");sales.save(sale);return sale;
    }
    void settle(CashierSale sale){
        Invoice inv=invoices.findById(sale.getInvoiceId()).orElseThrow();
        if(sale.getPaymentAmount()>0){PaymentRecord payment=new PaymentRecord();payment.setInvoiceId(inv.getId());payment.setInvoiceNumber(inv.getInvoiceNumber());payment.setAmount(sale.getPaymentAmount());payment.setPaymentMethod(sale.getPaymentMethod());payment.setRequestKey(sale.getId());payment.setNotes("INVOICE".equals(sale.getCheckoutType())?"Invoice payment":"Cashier POS");if("CARD".equals(sale.getPaymentMethod()))billing.processGatewayPayment(payment,sale.getGatewayPaymentId());else billing.processPayment(payment);}
        else if("SPARE_PART".equals(inv.getInvoiceType())){inv.setSettledTotal(0.0);inv.setSettledDate(LocalDate.now(clock));inv.setStatus("PAID");invoices.save(inv);notifications.invoiceChanged(inv,"Free parts invoice recorded");}
        else notifications.invoiceChanged(inv,"Additional parts added at the cashier");
        sale.setState("SUCCESS");sale.setCompletedAt(LocalDateTime.now(clock));sales.save(sale);
    }
    @Transactional(readOnly=true)
    public Object receipt(CashierSale sale){Invoice inv=sale.getInvoiceId()==null?null:invoices.findById(sale.getInvoiceId()).orElse(null);Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("completed","SUCCESS".equals(sale.getState()));result.put("saleId",sale.getId());result.put("state",sale.getState());result.put("paymentMethod",sale.getPaymentMethod());result.put("paymentDate",sale.getCompletedAt()!=null?sale.getCompletedAt().toLocalDate():sale.getRecordedAt()!=null?sale.getRecordedAt().toLocalDate():LocalDate.now(clock));result.put("invoice",inv);result.put("offerSavings",sale.getOfferSavings());result.put("paymentAmount",sale.getPaymentAmount());result.put("changeDue",sale.getChangeDue());result.put("message",sale.getMessage());return result;}
    @Transactional(readOnly=true)
    public Object checkoutStatus(String key,String cashier){CashierSale sale=sales.findById(requestKey(key)).orElse(null);if(sale==null)return Map.of("success",true,"completed",false,"state","NOT_FOUND");if(!cashier.equals(sale.getRecordedBy()))throw new org.springframework.security.access.AccessDeniedException("This checkout belongs to another cashier");return receipt(sale);}
    @Transactional(readOnly=true)
    public Object today(){LocalDate today=LocalDate.now(clock);Set<Long> processed=sales.findAll().stream().filter(s->s.getRecordedAt()!=null&&today.equals(s.getRecordedAt().toLocalDate())).map(CashierSale::getInvoiceId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());return Map.of("stationDate",today,"serverTime",Instant.now(clock),"nextRefreshAt",today.plusDays(1).atStartOfDay(clock.getZone()).toInstant(),"invoices",billing.getAllInvoices().stream().filter(i->today.equals(i.getInvoiceDate())||processed.contains(i.getId())).sorted(Comparator.comparing(Invoice::getId).reversed()).toList());}
}
