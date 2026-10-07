package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRecordRepository paymentRecordRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private SparePartRepository sparePartRepository;
    @Autowired private ServiceBookingRepository bookingRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private AppUserRepository userRepository;
    @Autowired private JobPartUsageRepository usageRepository;
    @Autowired private RefundRecordRepository refundRepository;
    @Autowired private NotificationService notifications;
    @Autowired private OfferService offerService;
    @Autowired private FuelInventoryRepository fuelInventories;
    @Autowired private Clock clock;

    private String number(String type) { return type + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT); }
    @Override @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() { return invoiceRepository.findAll(); }
    @Override @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(Long id) { return invoiceRepository.findById(id); }
    @Override
    public Invoice saveInvoice(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) invoice.setInvoiceNumber(number("SPARE_PART".equals(invoice.getInvoiceType()) ? "SP" : "FUEL".equals(invoice.getInvoiceType()) ? "FUEL" : "SRV"));
        if (invoice.getInvoiceDate() == null) invoice.setInvoiceDate(LocalDate.now(clock));
        if (invoice.getInvoiceType() == null) invoice.setInvoiceType(invoice.getReferenceNumber() == null ? "SPARE_PART" : "SERVICE");
        if (invoice.getAmountPaid() == null) invoice.setAmountPaid(0.0);
        if (invoice.getRefundedAmount() == null) invoice.setRefundedAmount(0.0);
        Rules.nonnegative(invoice.getNetTotal(), "Invoice total");
        return invoiceRepository.save(invoice);
    }
    @Override
    public Invoice createAdvanceInvoice(ServiceBooking booking, double amount) {
        // The advance is credited through payments on this single service invoice.
        return updateServiceInvoice(booking);
    }
    @Override
    public Invoice updateServiceInvoice(ServiceBooking booking) {
        String ref = Rules.required(booking.getReferenceNumber(), "Service reference");
        Optional<Invoice> found = invoiceRepository.findByReferenceNumber(ref);
        boolean creating = found.isEmpty();
        Invoice inv = found.orElseGet(Invoice::new);
        if (!creating) inv = invoiceRepository.lockByInvoiceNumber(inv.getInvoiceNumber()).orElseThrow();
        if ("REFUNDED".equals(inv.getStatus()) || "PARTIALLY_REFUNDED".equals(inv.getStatus()) || "VOID".equals(inv.getStatus()))
            throw new IllegalStateException("A closed invoice cannot be changed");
        double base = Rules.nonnegative(booking.getEstimatedCost(), "Service price");
        List<JobPartUsage> usages = usageRepository.findByReferenceNumber(ref);
        double parts = usages.stream().mapToDouble(u -> Rules.amount(u.getTotalAmount())).sum();
        double gross = Rules.money(base + parts);
        double discount = Rules.amount(inv.getDiscountAmount());
        if (discount > gross) throw new IllegalArgumentException("Discount exceeds the invoice total");
        double net = Rules.money(gross - discount);
        if (inv.getSettledTotal() != null && Math.abs(net - Rules.amount(inv.getNetTotal())) > 0.001) {
            if (net < Rules.amount(inv.getAmountPaid())) throw new IllegalStateException("Refund existing payments before reducing a settled invoice");
            inv.setSettledTotal(null);
            inv.setSettledDate(null);
            inv.setStatus("PARTIAL");
        }
        inv.setReferenceNumber(ref);
        inv.setInvoiceType("SERVICE");
        inv.setInvoiceName("Service: " + booking.getServiceType());
        inv.setCustomerUsername(booking.getCustomerUsername());
        inv.setCustomerName(booking.getCustomerName());
        inv.setCustomerType(booking.getCustomerType());
        inv.setLicensePlate(booking.getLicensePlate());
        inv.setServiceBaseAmount(base);
        inv.setPartsAmount(Rules.money(parts));
        inv.setGrossTotalAmount(gross);
        inv.setConvenienceFee(0.0);
        inv.setNetTotal(net);
        inv.setFinalized("Completed".equals(booking.getStatus()));
        if (Boolean.TRUE.equals(inv.getFinalized()) && inv.getPaymentStartDate() == null) { inv.setPaymentStartDate(LocalDate.now(clock)); inv.setDueDate(inv.getMaxDueDate()); }
        inv.setAdvanceAmountDue(("Full Service".equalsIgnoreCase(booking.getServiceType()) || "Repair".equalsIgnoreCase(booking.getServiceType())) ? Math.min(5000.0, net) : 0.0);
        if (creating) {
            inv.setInvoiceNumber(number("SRV"));
            inv.setInvoiceDate(LocalDate.now(clock));
            inv.setDueDate(inv.getMaxDueDate());
            inv.setAmountPaid(0.0);
            inv.setRefundedAmount(0.0);
            inv.setStatus("PENDING");
        }
        List<InvoiceItem> items = new ArrayList<>();
        items.add(item(booking.getServiceType(), null, 1, base));
        for (JobPartUsage u : usages) items.add(item(u.getPartName(), u.getPartId(), u.getQuantity(), u.getUnitPrice()));
        inv.setLineItems(items);
        Invoice saved = saveInvoice(inv);
        if (creating) notifications.invoiceChanged(saved, "Service invoice created");
        return saved;
    }
    private InvoiceItem item(String name, Long partId, int qty, double unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setItemName(name); item.setPartId(partId); item.setQuantity(qty); item.setUnitPrice(unitPrice); item.setTotalAmount(Rules.money(qty * unitPrice));
        return item;
    }
    @Override
    public void closeExpiredServiceInvoice(ServiceBooking booking) {
        if (!"Cancelled".equals(booking.getStatus())) throw new IllegalStateException("Cancel the expired service before closing its invoice");
        Invoice existing = invoiceRepository.findByReferenceNumber(booking.getReferenceNumber()).orElse(null);
        if (existing == null) return;
        Invoice inv = invoiceRepository.lockByInvoiceNumber(existing.getInvoiceNumber()).orElseThrow();
        if (Set.of("VOID", "REFUNDED").contains(inv.getStatus())) return;
        double retained = Rules.amount(inv.getAmountPaid());
        String reason = "Unstarted service automatically cancelled after its scheduled end: " + booking.getReferenceNumber();
        if (retained > 0.001) {
            List<PaymentRecord> payments = paymentRecordRepository.findByInvoiceNumberOrderByIdAsc(inv.getInvoiceNumber());
            double available = payments.stream().mapToDouble(p -> Math.max(0, Rules.amount(p.getAmount()) - Rules.amount(p.getRefundedAmount()))).sum();
            if (Math.abs(available - retained) > 0.001) throw new IllegalStateException("Invoice payments do not reconcile; the automatic refund needs review");
            for (PaymentRecord row : payments) {
                PaymentRecord payment = paymentRecordRepository.lockById(row.getId()).orElseThrow();
                double amount = Rules.money(Math.max(0, Rules.amount(payment.getAmount()) - Rules.amount(payment.getRefundedAmount())));
                if (amount > 0.001) refundPart(inv, payment, amount, reason, "expired-service-" + inv.getId() + "-" + payment.getId());
            }
            finishRefund(inv, retained, reason, true);
        } else {
            inv.setStatus(Rules.amount(inv.getRefundedAmount()) > 0 ? "REFUNDED" : "VOID");
            invoiceRepository.save(inv);
            notifications.invoiceChanged(inv, "Expired service invoice closed");
        }
    }
    @Override
    public Invoice checkoutService(String ref, Double discount, String method, Double amount, Double cash, String key) {
        ServiceBooking booking = bookingRepository.findByReferenceNumber(Rules.required(ref, "Service reference"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if ("Cancelled".equals(booking.getStatus())) throw new IllegalStateException("A cancelled booking cannot be billed");
        Invoice inv = updateServiceInvoice(booking);
        double requestedDiscount = discount == null ? Rules.amount(inv.getDiscountAmount()) : Rules.nonnegative(discount, "Discount");
        if (requestedDiscount > inv.getGrossTotalAmount()) throw new IllegalArgumentException("Discount exceeds the invoice total");
        if (Math.abs(requestedDiscount - Rules.amount(inv.getDiscountAmount())) > 0.001) {
            if (Rules.amount(inv.getAmountPaid()) > 0) throw new IllegalStateException("An invoice discount cannot change after a payment");
            inv.setDiscountAmount(requestedDiscount);
            inv.setNetTotal(Rules.money(inv.getGrossTotalAmount() - requestedDiscount));
            invoiceRepository.save(inv);
        }
        double paid = amount == null ? 0 : Rules.nonnegative(amount, "Payment amount");
        if (paid > 0) {
            String normalizedMethod = Rules.paymentMethod(method);
            if ("CASH".equals(normalizedMethod) && Rules.nonnegative(cash, "Cash tendered") + 0.001 < paid) throw new IllegalArgumentException("Cash tendered is below the payment amount");
            PaymentRecord record = new PaymentRecord();
            record.setInvoiceNumber(inv.getInvoiceNumber()); record.setAmount(paid); record.setPaymentMethod(normalizedMethod); record.setRequestKey(key);
            processPayment(record);
        }
        return invoiceRepository.findByInvoiceNumber(inv.getInvoiceNumber()).orElseThrow();
    }
    @Override
    public Invoice createSparePartInvoice(SparePart part, Integer quantity, String username, String method, Double cash) {
        Rules.quantity(quantity, "Quantity");
        return purchasePart(part.getId(), quantity, username, null, null, method, cash, null);
    }
    @Override
    public Invoice purchasePart(Long id, Integer quantity, String username, String plate, String name, String method, Double cash, String key) {
        int qty = Rules.quantity(quantity, "Quantity");
        SparePart part = sparePartRepository.lockById(id).orElseThrow(() -> new IllegalArgumentException("Spare part not found"));
        String normalizedPlate=plate==null||plate.isBlank()?null:plate.trim().toUpperCase(Locale.ROOT);
        Vehicle selectedVehicle=normalizedPlate==null?null:vehicleRepository.findByLicensePlate(normalizedPlate).orElseThrow(() -> new IllegalArgumentException("Registered vehicle not found"));
        if ((username==null||username.isBlank()) && selectedVehicle!=null) username=selectedVehicle.getOwnerUsername();
        AppUser selectedCustomer=username==null||username.isBlank()?null:userRepository.findByUsernameIgnoreCase(username.trim()).filter(u -> "Customer".equalsIgnoreCase(u.getRole()) && "Active".equals(u.getStatus())).orElseThrow(() -> new IllegalArgumentException("Select an active customer account"));
        String owner=selectedCustomer==null?null:selectedCustomer.getUsername();
        String paymentMethod = Rules.paymentMethod(method == null ? "CASH" : method);
        if (key != null && !key.isBlank()) {
            if(key.length()>128)throw new IllegalArgumentException("Invalid purchase request key");
            Optional<Invoice> prior=invoiceRepository.findByPurchaseRequestKey(key);
            if(prior.isPresent()){
                Invoice previous=prior.get();
                if(!Objects.equals(previous.getCustomerUsername(),owner)||!Objects.equals(previous.getLicensePlate(),normalizedPlate)||!Objects.equals(previous.getPaymentMethod(),paymentMethod)||previous.getLineItems().stream().noneMatch(i -> Objects.equals(i.getPartId(),id)&&Objects.equals(i.getQuantity(),qty)))throw new IllegalArgumentException("Purchase request key was already used for different details");
                return previous;
            }
        }
        if ("Inactive".equalsIgnoreCase(part.getStatus()) || "Deactivated".equalsIgnoreCase(part.getStatus())) throw new IllegalArgumentException("This part is not available for sale");
        if (part.getStockQuantity() == null || part.getStockQuantity() < qty) throw new IllegalArgumentException("Not enough stock");
        double unit = Rules.nonnegative(part.getSellingPrice(), "Selling price");
        for (Offer offer : offerService.getActiveOffersByType(OfferTargetType.SPARE_PART)) {
            if (Objects.equals(offer.getTargetId(), id)) { unit = Rules.money(unit * (1 - Rules.nonnegative(offer.getDiscountPercentage(), "Discount") / 100)); break; }
        }
        double total = Rules.nonnegative(unit * qty, "Purchase total");
        if ("CASH".equals(paymentMethod) && Rules.nonnegative(cash, "Cash tendered") + 0.001 < total) throw new IllegalArgumentException("Cash tendered must cover the purchase total");
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(number("SP")); inv.setInvoiceType("SPARE_PART");
        inv.setInvoiceName("Spare part: " + part.getPartName() + " x" + qty);
        inv.setCustomerName(name == null || name.isBlank() ? "Guest Customer" : name.trim());
        inv.setCustomerType("GUEST_CUSTOMER");
        inv.setPurchaseRequestKey(key==null||key.isBlank()?null:key);
        if(selectedVehicle!=null){inv.setLicensePlate(normalizedPlate);inv.setCustomerName(selectedVehicle.getOwnerName());inv.setCustomerType("REGISTERED_VEHICLE");}
        if(selectedCustomer!=null){inv.setCustomerUsername(owner);inv.setCustomerName(selectedCustomer.getFullName());inv.setCustomerType("REGISTERED_CUSTOMER");}
        inv.setInvoiceDate(LocalDate.now(clock)); inv.setDueDate(LocalDate.now(clock));
        inv.setGrossTotalAmount(total); inv.setConvenienceFee(0.0); inv.setNetTotal(total); inv.setAmountPaid(0.0); inv.setStatus("PENDING");
        inv.setPaymentMethod(paymentMethod); inv.setCashTendered(cash); inv.setFinalized(true);
        inv.setLineItems(new ArrayList<>(List.of(item(part.getPartName(), id, qty, unit))));
        saveInvoice(inv);
        part.setStockQuantity(part.getStockQuantity() - qty); updateStockStatus(part); sparePartRepository.save(part);
        PaymentRecord record = new PaymentRecord(); record.setInvoiceNumber(inv.getInvoiceNumber()); record.setAmount(total); record.setPaymentMethod(paymentMethod); record.setRequestKey(key); record.setNotes("Spare part purchase");
        if(total>0) processPayment(record);
        else {inv.setSettledTotal(0.0);inv.setSettledDate(LocalDate.now(clock));inv.setStatus("PAID");invoiceRepository.save(inv);notifications.invoiceChanged(inv,"Parts invoice created");}
        return inv;
    }
    @Override @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(String username) { return username == null || username.isBlank() ? List.of() : invoiceRepository.findByCustomerUsernameOrderByInvoiceDateDesc(username.trim()); }
    @Override @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceForCustomer(String number, String username) {
        return number == null || username == null ? Optional.empty() : invoiceRepository.findByInvoiceNumber(number).filter(i -> username.equalsIgnoreCase(i.getCustomerUsername()));
    }
    @Override
    public PaymentRecord processPayment(PaymentRecord payment) {
        String number = payment.getInvoiceNumber();
        if (number == null || number.isBlank()) {
            if (payment.getInvoiceId() != null) number = invoiceRepository.findById(payment.getInvoiceId()).orElseThrow(() -> new IllegalArgumentException("Invoice not found")).getInvoiceNumber();
            else number = Rules.required(payment.getReferenceNumber(), "Invoice number");
        }
        Invoice inv = invoiceRepository.lockByInvoiceNumber(number).orElseThrow(() -> new IllegalArgumentException("Invoice number not found"));
        if (payment.getInvoiceId() != null && !Objects.equals(inv.getId(), payment.getInvoiceId())) throw new IllegalArgumentException("Invoice identifiers do not match");
        double amount = Rules.positive(payment.getAmount(), "Payment amount");
        String method = Rules.paymentMethod(payment.getPaymentMethod());
        if (payment.getStatus() != null && !"SUCCESS".equals(payment.getStatus())) throw new IllegalArgumentException("Only successful payments can settle an invoice");
        if (payment.getRequestKey() != null && !payment.getRequestKey().isBlank()) {
            if (payment.getRequestKey().length() > 128) throw new IllegalArgumentException("Invalid payment request key");
            Optional<PaymentRecord> prior = paymentRecordRepository.findByRequestKey(payment.getRequestKey());
            if (prior.isPresent()) {
                PaymentRecord old = prior.get();
                if (!number.equals(old.getInvoiceNumber()) || Math.abs(amount - old.getAmount()) > 0.001 || !method.equals(old.getPaymentMethod())) throw new IllegalArgumentException("Payment request key has already been used");
                return old;
            }
        }
        double balance = inv.getBalanceDue();
        if (balance <= 0.01 || "VOID".equals(inv.getStatus())) throw new IllegalArgumentException("Invoice has no outstanding balance");
        if (amount > balance + 0.001) throw new IllegalArgumentException("Payment exceeds the remaining balance");
        if ("SERVICE".equals(inv.getInvoiceType()) && !Boolean.TRUE.equals(inv.getFinalized()) && amount >= balance - 0.001) throw new IllegalArgumentException("Complete the service before full settlement. Advance or partial payments can be recorded now.");
        if ("SPARE_PART".equals(inv.getInvoiceType()) && amount + 0.001 < balance) throw new IllegalArgumentException("Spare parts require full payment");
        payment.setId(null); payment.setVersion(null); payment.setAmount(amount); payment.setInvoiceId(inv.getId()); payment.setInvoiceNumber(number); payment.setReferenceNumber(inv.getReferenceNumber());
        payment.setCustomerUsername(inv.getCustomerUsername()); payment.setPaidOccupant(inv.getCustomerName()); payment.setPaymentDate(LocalDate.now(clock)); payment.setPaymentMethod(method); payment.setStatus("SUCCESS"); payment.setRefundedAmount(0.0);
        double total = inv.getNetTotalWithPenalty();
        inv.setAmountPaid(Rules.money(Rules.amount(inv.getAmountPaid()) + amount)); inv.setPaymentMethod(method);
        if (total - inv.getAmountPaid() <= 0.01) { inv.setSettledTotal(total); inv.setSettledDate(LocalDate.now(clock)); inv.setStatus("PAID"); }
        else inv.setStatus("PARTIAL");
        invoiceRepository.save(inv);
        PaymentRecord saved = paymentRecordRepository.save(payment);
        notifications.invoiceChanged(inv, "Payment recorded");
        return saved;
    }
    @Override
    public PaymentRecord processCustomerPayment(String number, String username, String method, Double amount) {
        return processCustomerPayment(number, username, method, amount, null);
    }
    @Override
    public PaymentRecord processCustomerPayment(String number, String username, String method, Double amount, String key) {
        getInvoiceForCustomer(number, username).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Invoice is not yours"));
        PaymentRecord p = new PaymentRecord(); p.setInvoiceNumber(number); p.setPaymentMethod(method); p.setAmount(amount); p.setRequestKey(key);
        return processPayment(p);
    }
    @Override @Transactional(readOnly = true)
    public List<PaymentRecord> getPaymentsByCustomer(String username) { return username == null ? List.of() : paymentRecordRepository.findByCustomerUsernameOrderByPaymentDateDesc(username); }
    @Override @Transactional(readOnly = true)
    public List<PaymentRecord> getAllPayments() { return paymentRecordRepository.findAllByOrderByPaymentDateDesc(); }
    @Override @Transactional(readOnly = true)
    public List<PaymentRecord> getPaymentsByReferenceNumber(String number) {
        if (number == null) return List.of();
        Optional<Invoice> inv = invoiceRepository.findByInvoiceNumber(number).or(() -> invoiceRepository.findByReferenceNumber(number));
        return inv.map(i -> paymentRecordRepository.findByInvoiceNumberOrderByIdAsc(i.getInvoiceNumber())).orElse(List.of());
    }
    @Override
    public boolean refundPayment(Long id, String reason) {
        PaymentRecord original = paymentRecordRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Invoice inv = invoiceRepository.lockByInvoiceNumber(original.getInvoiceNumber()).orElseThrow();
        PaymentRecord p = paymentRecordRepository.lockById(id).orElseThrow();
        double remaining = Rules.money(Rules.amount(p.getAmount()) - Rules.amount(p.getRefundedAmount()));
        if (remaining <= 0 || "REFUNDED".equals(p.getStatus())) return false;
        boolean closed = inv.getBalanceDue() <= 0.01;
        refundPart(inv, p, remaining, Rules.required(reason, "Refund reason"), "payment-refund-" + id);
        finishRefund(inv, remaining, reason, closed);
        return true;
    }
    @Override
    public boolean refundInvoice(String number, Double amount, String reason) { return refundInvoice(number, amount, reason, null); }
    @Override
    public boolean refundInvoice(String number, Double amount, String reason, String key) {
        Invoice inv = invoiceRepository.lockByInvoiceNumber(number).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (key != null) {
            if (key.isBlank() || key.length() > 100) throw new IllegalArgumentException("Invalid refund request key");
            var old = refundRepository.findByRequestKey(key);
            if (old.isPresent()) { if (!number.equals(old.get().getInvoiceNumber()) || amount != null && Math.abs(amount-old.get().getRequestedAmount())>.001 || !Objects.equals(reason, old.get().getReason())) throw new IllegalArgumentException("Refund request key has already been used"); return true; }
        }
        if (inv.getBalanceDue() > 0.01 || "VOID".equals(inv.getStatus())) return false;
        double refund = Rules.positive(amount == null ? inv.getAmountPaid() : amount, "Refund amount");
        if (refund > Rules.amount(inv.getAmountPaid()) + 0.001) throw new IllegalArgumentException("Refund exceeds retained payments");
        String why = Rules.required(reason, "Refund reason");
        List<PaymentRecord> payments = paymentRecordRepository.findByInvoiceNumberOrderByIdAsc(number);
        double available = payments.stream().mapToDouble(p -> Math.max(0, Rules.amount(p.getAmount()) - Rules.amount(p.getRefundedAmount()))).sum();
        if (available + 0.001 < refund) throw new IllegalStateException("Invoice payments do not reconcile; no refund was recorded");
        double left = refund;
        int index = 0;
        for (PaymentRecord row : payments) {
            if (left <= 0.001) break;
            PaymentRecord p = paymentRecordRepository.lockById(row.getId()).orElseThrow();
            double retained = Math.max(0, Rules.amount(p.getAmount()) - Rules.amount(p.getRefundedAmount()));
            double portion = Math.min(left, retained);
            if (portion > 0) { refundPart(inv, p, portion, why, key == null ? null : key + ":" + index++); left = Rules.money(left - portion); }
        }
        if (key != null) { RefundRecord marker = new RefundRecord(); marker.setInvoiceNumber(number); marker.setAmount(0.0); marker.setRequestedAmount(refund); marker.setRequestKey(key); marker.setReason(why); marker.setCreatedAt(LocalDateTime.now(clock)); refundRepository.save(marker); }
        finishRefund(inv, refund, why, true);
        return true;
    }
    private void refundPart(Invoice inv, PaymentRecord p, double amount, String reason, String key) {
        p.setRefundedAmount(Rules.money(Rules.amount(p.getRefundedAmount()) + amount));
        p.setStatus(p.getAmount() - p.getRefundedAmount() <= 0.01 ? "REFUNDED" : "PARTIALLY_REFUNDED");
        paymentRecordRepository.save(p);
        RefundRecord refund = new RefundRecord(); refund.setInvoiceNumber(inv.getInvoiceNumber()); refund.setPaymentId(p.getId()); refund.setAmount(amount); refund.setReason(reason); refund.setCreatedAt(LocalDateTime.now(clock)); refund.setRequestKey(key);
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(); refund.setRecordedBy(auth == null ? "system" : auth.getName()); refundRepository.save(refund);
    }
    private void finishRefund(Invoice inv, double amount, String reason, boolean closed) {
        if (closed && inv.getSettledTotal() == null) { inv.setSettledTotal(inv.getNetTotalWithPenalty()); inv.setSettledDate(LocalDate.now(clock)); }
        inv.setAmountPaid(Rules.money(Rules.amount(inv.getAmountPaid()) - amount)); inv.setRefundedAmount(Rules.money(Rules.amount(inv.getRefundedAmount()) + amount)); inv.setRefundReason(reason); inv.setRefundDate(LocalDate.now(clock));
        inv.setStatus(closed ? (inv.getAmountPaid() <= 0.01 ? "REFUNDED" : "PARTIALLY_REFUNDED") : (inv.getAmountPaid() > 0 ? "PARTIAL" : "PENDING"));
        invoiceRepository.save(inv); notifications.invoiceChanged(inv, "Refund recorded");
    }
    @Override
    public PaymentRecord updatePaymentStatus(Long id, String status) {
        if ("REFUNDED".equals(status)) { refundPayment(id, "Staff payment refund"); return paymentRecordRepository.findById(id).orElseThrow(); }
        throw new IllegalArgumentException("Recorded settlements cannot be edited; use a refund");
    }
    @Override @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() { return supplierRepository.findAll(); }
    @Override @Transactional(readOnly = true)
    public Optional<Supplier> getSupplierById(Long id) { return supplierRepository.findById(id); }
    @Override
    public Supplier saveSupplier(Supplier supplier) {
        supplier.setSupplierName(Rules.required(supplier.getSupplierName(), "Supplier name"));
        if (supplierRepository.findAll().stream().anyMatch(s -> !Objects.equals(s.getId(),supplier.getId()) && s.getSupplierName().equalsIgnoreCase(supplier.getSupplierName()))) throw new IllegalArgumentException("Supplier name already exists");
        if (supplier.getId()!=null) {
            var old=supplierRepository.findById(supplier.getId()).orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
            if(!Objects.equals(old.getSupplierName(),supplier.getSupplierName()) && (sparePartRepository.findAll().stream().anyMatch(p -> old.getSupplierName().equals(p.getSupplier())) || fuelInventories.findAll().stream().anyMatch(t -> old.getSupplierName().equals(t.getSupplier())))) throw new IllegalStateException("Keep the supplier name to preserve linked inventory records");
        }
        supplier.setSupplierName(Rules.required(supplier.getSupplierName(), "Supplier name"));
        supplier.setCompany(Rules.required(supplier.getCompany(), "Company")); supplier.setContactNumber(Rules.required(supplier.getContactNumber(), "Contact number"));
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank() && !supplier.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Enter a valid supplier email");
        if (supplier.getStatus() == null) supplier.setStatus("Active");
        if (!Set.of("Active","Inactive").contains(supplier.getStatus())) throw new IllegalArgumentException("Select a valid supplier status");
        return supplierRepository.save(supplier);
    }
    @Override
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        if (sparePartRepository.findAll().stream().anyMatch(p -> Objects.equals(p.getSupplier(), supplier.getSupplierName())) || fuelInventories.findAll().stream().anyMatch(t -> supplier.getSupplierName().equals(t.getSupplier()))) throw new IllegalStateException("Supplier has linked parts. Deactivate the supplier instead.");
        supplierRepository.deleteById(id);
    }
    @Override @Transactional(readOnly = true)
    public List<SparePart> getAllSpareParts() { return sparePartRepository.findAll(); }
    @Override @Transactional(readOnly = true)
    public Optional<SparePart> getSparePartById(Long id) { return sparePartRepository.findById(id); }
    @Override
    public SparePart saveSparePart(SparePart input) {
        SparePart part = input.getId() == null ? new SparePart() : sparePartRepository.lockById(input.getId()).orElseThrow(() -> new IllegalArgumentException("Spare part not found"));
        if (input.getVersion()!=null && !Objects.equals(input.getVersion(),part.getVersion())) throw new IllegalStateException("Part stock changed while this form was open. Refresh the part and try again.");
        String savedImage = part.getImageUrl();
        org.springframework.beans.BeanUtils.copyProperties(input, part, "id", "version");
        if (input.getImageUrl() == null) part.setImageUrl(savedImage);
        part.setPartName(Rules.required(part.getPartName(), "Part name"));
        if (part.getSupplier()!=null && !part.getSupplier().isBlank() && supplierRepository.findAll().stream().noneMatch(s -> s.getSupplierName().equals(part.getSupplier()))) throw new IllegalArgumentException("Select an existing supplier");
        if (part.getStockQuantity() == null || part.getStockQuantity() < 0 || part.getMinStockWarning() == null || part.getMinStockWarning() < 0) throw new IllegalArgumentException("Stock and warning levels must be nonnegative whole numbers");
        Rules.nonnegative(part.getBuyingPrice(), "Buying price"); Rules.nonnegative(part.getSellingPrice(), "Selling price");
        updateStockStatus(part); return sparePartRepository.save(part);
    }
    private void updateStockStatus(SparePart part) {
        part.setStatus(part.getStockQuantity() <= Rules.amount(part.getMinStockWarning() == null ? null : part.getMinStockWarning().doubleValue()) ? "Low Stock" : "Available");
        if ("Low Stock".equals(part.getStatus())) notifications.notifyRoles("Spare part stock low", part.getPartName() + " has " + part.getStockQuantity() + " units remaining", "Inventory", "/dashboard/inventory/parts", "MANAGER");
        if ("Low Stock".equals(part.getStatus())) notifications.notifyRoles("Parts stock low",part.getPartName(),"Inventory","/dashboard/pos","CASHIER");
    }
    @Override
    public void deleteSparePart(Long id) {
        if (offerService.getAllOffersByType(OfferTargetType.SPARE_PART).stream().anyMatch(o -> id.equals(o.getTargetId()))) throw new IllegalStateException("Delete this part's offers first");
        if (usageRepository.existsByPartId(id) || invoiceRepository.findAll().stream().anyMatch(i -> i.getLineItems().stream().anyMatch(l -> Objects.equals(l.getPartId(), id)))) throw new IllegalStateException("This part is used by a job or invoice; retain its history instead of deleting it");
        sparePartRepository.deleteById(id);
    }
}
