package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class Invoice {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="reference_number", referencedColumnName="reference_number", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private ServiceBooking booking;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(name="reference_number", unique=true)
    private String referenceNumber;
    @jakarta.persistence.Column(unique = true)
    private String invoiceNumber; // Every invoice; separate from the service reference.
    @jakarta.persistence.Column(unique=true)
    private String purchaseRequestKey;
    private String invoiceType; // SERVICE or SPARE_PART
    private String invoiceName;
    private String customerName; // "Paid Occupant"
    
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    
    private Double grossTotalAmount;
    private Double convenienceFee;
    private Double netTotal;
    
    private Double amountPaid; // To track split payments
    
    // Refund tracking
    private Double refundedAmount;
    private String refundReason;
    private LocalDate refundDate;
    
    private String status; // PAID, OVERDUE, PARTIAL, PENDING, REFUNDED

    // Customer & Service References
    private String customerUsername;
    private String licensePlate;
    private String notes;

    // POS Cashier Enhancements
    private String customerType; // REGISTERED_CUSTOMER, REGISTERED_VEHICLE, GUEST_CUSTOMER
    private String paymentMethod; // Cash, Card, QR
    private Double overduePenaltyRate; // default 2.5% per day
    private Double cashTendered;
    private Double serviceBaseAmount;
    private Double partsAmount;
    private Double discountAmount;
    private Double advanceAmountDue;
    private Boolean finalized;
    private LocalDate paymentStartDate;
    private LocalDate settledDate;
    private Double settledTotal;

    @jakarta.persistence.Version
    private Long version;
    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    private java.util.List<InvoiceItem> lineItems = new java.util.ArrayList<>();

    private LocalDate calculationDate() {
        return settledDate != null ? settledDate : LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
    }
    private boolean legacySettled() {
        return settledDate == null && ("PAID".equalsIgnoreCase(status) || "REFUNDED".equalsIgnoreCase(status));
    }
    public Double getTotalAmount() { return netTotal != null ? netTotal : (grossTotalAmount != null ? grossTotalAmount : 0.0); }
    public Double getBalanceDue() {
        if (settledTotal != null || "REFUNDED".equalsIgnoreCase(status) || "PARTIALLY_REFUNDED".equalsIgnoreCase(status) || "VOID".equalsIgnoreCase(status)) return 0.0;
        return com.fuelstation.util.Rules.money(Math.max(0, getNetTotalWithPenalty() - (amountPaid == null ? 0 : amountPaid)));
    }
    public String getStatus() { return getCalculatedStatus(); }
    public String getCalculatedStatus() {
        if ("VOID".equalsIgnoreCase(status)) return "VOID";
        if ("REFUNDED".equalsIgnoreCase(status)) return "REFUNDED";
        if ("PARTIALLY_REFUNDED".equalsIgnoreCase(status)) return "PARTIALLY_REFUNDED";
        if (getBalanceDue() <= 0.01) return "SERVICE".equals(invoiceType)&&!Boolean.TRUE.equals(finalized)?"PENDING":"PAID";
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
        if (dueDate != null && dueDate.isBefore(today)) return "OVERDUE";
        return amountPaid != null && amountPaid > 0 ? "PARTIAL" : "PENDING";
    }
    public Double getOverduePenalty() {
        if (("SERVICE".equals(invoiceType) && !Boolean.TRUE.equals(finalized)) || legacySettled() || "VOID".equalsIgnoreCase(status) || dueDate == null || grossTotalAmount == null) return 0.0;
        LocalDate today = calculationDate();
        if (!today.isAfter(dueDate)) return 0.0;
        double rate = overduePenaltyRate != null && overduePenaltyRate > 0 ? overduePenaltyRate : 2.5;
        return com.fuelstation.util.Rules.money(grossTotalAmount * rate / 100 * ChronoUnit.DAYS.between(dueDate, today));
    }
    public Double getPreOverdueInterest() {
        if (("SERVICE".equals(invoiceType) && (!Boolean.TRUE.equals(finalized) || paymentStartDate == null)) || legacySettled() || "VOID".equalsIgnoreCase(status) || invoiceDate == null || grossTotalAmount == null) return 0.0;
        LocalDate date = calculationDate();
        if (dueDate != null && date.isAfter(dueDate)) date = dueDate;
        LocalDate start = "SERVICE".equals(invoiceType) ? paymentStartDate : invoiceDate;
        if (!date.isAfter(start)) return 0.0;
        return com.fuelstation.util.Rules.money(grossTotalAmount * 0.01 * ChronoUnit.DAYS.between(start, date));
    }
    public Double getNetTotalWithPenalty() {
        if (settledTotal != null) return settledTotal;
        return com.fuelstation.util.Rules.money(getTotalAmount() + getPreOverdueInterest() + getOverduePenalty());
    }

    /**
     * Returns the maximum allowed due date based on customer type.
     */
    public LocalDate getMaxDueDate() {
        if ("SERVICE".equals(invoiceType) && paymentStartDate == null) return null;
        LocalDate issue = "SERVICE".equals(invoiceType) ? paymentStartDate : invoiceDate != null ? invoiceDate : LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
        if ("REGISTERED_CUSTOMER".equals(customerType)) {
            return issue.plusDays(7);
        } else if ("REGISTERED_VEHICLE".equals(customerType)) {
            return issue.plusDays(3);
        }
        return issue; // GUEST_CUSTOMER: due date = issue date
    }
}
