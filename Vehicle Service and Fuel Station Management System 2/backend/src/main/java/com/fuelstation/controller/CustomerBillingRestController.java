package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerBillingRestController {

    @Autowired
    private BillingService billingService;
    
    
    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    // Helper DTO to ensure dynamic getters are serialized
    public static class InvoiceDTO {
        public Long id;
        public String referenceNumber;
        public String invoiceNumber;
        public String invoiceType;
        public String invoiceName;
        public String licensePlate;
        public java.time.LocalDate invoiceDate;
        public java.time.LocalDate dueDate;
        public java.time.LocalDate paymentStartDate;
        public Double grossTotalAmount;
        public Double convenienceFee;
        public Double netTotal;
        public Double amountPaid;
        public String status;
        
        public Double overduePenalty;
        public Double preOverdueInterest;
        public Double netTotalWithPenalty;
        public String calculatedStatus;
        public Double balanceDue;
        public Double refundedAmount;
        public Double discountAmount;
        public Double advanceAmountDue;
        public Boolean finalized;
        public String customerName;
        public String customerType;
        public List<InvoiceItem> lineItems;

        public InvoiceDTO(Invoice i) {
            this.id = i.getId();
            this.referenceNumber = i.getReferenceNumber();
            this.invoiceNumber = i.getInvoiceNumber();
            this.invoiceType = i.getInvoiceType();
            this.invoiceName = i.getInvoiceName();
            this.licensePlate = i.getLicensePlate();
            this.invoiceDate = i.getInvoiceDate();
            this.dueDate = i.getDueDate();
            this.paymentStartDate = i.getPaymentStartDate();
            this.grossTotalAmount = i.getGrossTotalAmount();
            this.convenienceFee = i.getConvenienceFee();
            this.netTotal = i.getNetTotal();
            this.amountPaid = i.getAmountPaid();
            this.status = i.getStatus();

            // Dynamic logic evaluation
            this.overduePenalty = i.getOverduePenalty();
            this.preOverdueInterest = i.getPreOverdueInterest();
            this.netTotalWithPenalty = i.getNetTotalWithPenalty();
            this.calculatedStatus = i.getCalculatedStatus();
            this.balanceDue = i.getBalanceDue();
            this.refundedAmount = i.getRefundedAmount();
            this.discountAmount = i.getDiscountAmount();
            this.advanceAmountDue = i.getAdvanceAmountDue();
            this.finalized = i.getFinalized();
            this.customerName = i.getCustomerName();
            this.customerType = i.getCustomerType();
            this.lineItems = i.getLineItems();
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> getInvoices(Authentication auth, @RequestParam(required = false) String status) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        
        List<Invoice> allInvoices = billingService.getInvoicesByCustomer(username);
        
        List<InvoiceDTO> dtos = allInvoices.stream()
            .map(InvoiceDTO::new)
            .collect(Collectors.toList());

        double totalAmountDue = 0.0;
        int paidCount = 0;
        int pendingCount = 0;

        for (InvoiceDTO dto : dtos) {
            if ("PAID".equals(dto.calculatedStatus)) {
                paidCount++;
            } else if (dto.balanceDue > 0.01) {
                pendingCount++;
                totalAmountDue += dto.balanceDue;
            }
        }

        Map<String, Object> summary = Map.of(
            "totalAmountDue", totalAmountDue,
            "paidCount", paidCount,
            "pendingCount", pendingCount
        );

        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) dtos = dtos.stream().filter(i -> i.calculatedStatus.equalsIgnoreCase(status)).toList();
        return ResponseEntity.ok(Map.of("summary", summary, "invoices", dtos));
    }

    @GetMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable String invoiceNumber, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<Invoice> invoiceOpt = billingService.getInvoiceForCustomer(invoiceNumber, username);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Invoice not found"));
        }
        
        Invoice invoice = invoiceOpt.get();
        List<PaymentRecord> payments = billingService.getPaymentsByReferenceNumber(invoice.getInvoiceNumber());
        
        InvoiceDTO dto = new InvoiceDTO(invoice);
        
        return ResponseEntity.ok(Map.of("invoice", dto, "payments", payments));
    }

    @PostMapping("/invoices/{invoiceNumber}/pay")
    public ResponseEntity<?> processPayment(@PathVariable String invoiceNumber, @RequestBody Map<String, Object> payload, Authentication auth, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        String paymentMethod = (String) payload.get("paymentMethod");
        Double amount = null;
        if (payload.get("amount") instanceof Number) {
            amount = ((Number) payload.get("amount")).doubleValue();
        }

        if (paymentMethod == null || amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid payment details"));
        }

        try {
            PaymentRecord record = billingService.processCustomerPayment(invoiceNumber, username, paymentMethod, amount, key);
            return ResponseEntity.ok(Map.of("success", true, "message", "Payment successful", "record", record));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getPaymentsLedger(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        List<PaymentRecord> payments = billingService.getPaymentsByCustomer(username);
        
        double totalSettled = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()) || "PARTIALLY_REFUNDED".equals(p.getStatus()))
                .mapToDouble(p -> Math.max(0, com.fuelstation.util.Rules.amount(p.getAmount()) - com.fuelstation.util.Rules.amount(p.getRefundedAmount())))
                .sum();

        return ResponseEntity.ok(Map.of(
            "summary", Map.of("totalSettled", totalSettled),
            "payments", payments
        ));
    }
}
