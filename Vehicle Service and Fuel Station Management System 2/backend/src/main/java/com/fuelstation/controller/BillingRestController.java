package com.fuelstation.controller;

import com.fuelstation.model.Invoice;
import com.fuelstation.model.PaymentRecord;
import com.fuelstation.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class BillingRestController {

    @Autowired
    private BillingService billingService;
    @Autowired private com.fuelstation.service.UserService userService;

    @GetMapping("/customer-accounts")
    public Object customerAccounts() {
        return userService.getCustomersOnly().stream().map(user -> Map.of("username", user.getUsername(), "fullName", user.getFullName(), "phoneNumber", user.getPhoneNumber() == null ? "" : user.getPhoneNumber())).toList();
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        return billingService.getInvoiceById(id).<ResponseEntity<?>>map(invoice -> ResponseEntity.ok(Map.of("invoice", invoice, "payments", billingService.getPaymentsByReferenceNumber(invoice.getInvoiceNumber())))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> getAllInvoices() {
        List<Invoice> invoices = billingService.getAllInvoices();
        
        // Dynamic getters supply totals/status without mutating the stored base amount.
        
        Map<String, Object> response = new HashMap<>();
        response.put("invoices", invoices);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getAllPayments() {
        List<PaymentRecord> payments = billingService.getAllPayments();
        Map<String, Object> response = new HashMap<>();
        response.put("payments", payments);
        
        // Calculate total settled
        double totalSettled = payments.stream()
            .filter(p -> "SUCCESS".equals(p.getStatus()) || "PARTIALLY_REFUNDED".equals(p.getStatus()))
            .mapToDouble(p -> Math.max(0, com.fuelstation.util.Rules.amount(p.getAmount()) - com.fuelstation.util.Rules.amount(p.getRefundedAmount())))
            .sum();
            
        response.put("totalSettled", totalSettled);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRecord payment, @RequestHeader(value = "Idempotency-Key", required = false) String key, org.springframework.security.core.Authentication authentication) {
        try {
            if (key != null) payment.setRequestKey(key);
            PaymentRecord recorded=billingService.processPayment(payment);
            return ResponseEntity.ok(Map.of("success",true,"message","Payment processed successfully","record",recorded,"invoice",billingService.getInvoiceById(recorded.getInvoiceId()).orElseThrow(),"paymentAmount",recorded.getAmount(),"paymentMethod",recorded.getPaymentMethod()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/refund/{id}")
    public ResponseEntity<?> processRefund(@PathVariable Long id, @RequestParam("reason") String reason, @RequestParam(value = "amount", required = false) String amount, @RequestParam(value="paymentMethod",required=false) String method, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        try {
            Double requested=amount==null?null:com.fuelstation.util.InputValidation.decimal(amount,"Refund amount",.01,999999999.99,false).doubleValue();
            Invoice inv = billingService.getInvoiceById(id).orElse(null);
            if (inv == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invoice not found"));
            }
            // Use referenceNumber if invoiceNumber is null (some invoices might use referenceNumber as ID)
            String invNumber = inv.getInvoiceNumber();
            Double refundAmount = requested != null ? requested : inv.getAmountPaid();
            boolean success = method==null?billingService.refundInvoice(invNumber, refundAmount, reason, key):billingService.refundInvoice(invNumber, refundAmount, reason, key, method);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Refund processed successfully", "invoice", billingService.getInvoiceById(id).orElse(inv)));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Refund failed or already refunded"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/refund-payment/{id}")
    public ResponseEntity<?> processPaymentRefund(@PathVariable Long id, @RequestParam("reason") String reason, @RequestParam(value="amount",required=false) String amount, @RequestParam(value="paymentMethod",required=false) String method, @RequestHeader(value="Idempotency-Key",required=false) String key) {
        try {
            Double refund=amount==null?null:com.fuelstation.util.InputValidation.decimal(amount,"Refund amount",.01,999999999.99,false).doubleValue();
            boolean success = method==null?billingService.refundPayment(id,refund,reason,key):billingService.refundPayment(id,refund,reason,key,method);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Payment refunded successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Refund failed or already refunded"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
