package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class FuelSale {
    // Historical records only: extra receipt creation and receipt-specific APIs are discontinued.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(unique=true)
    private String receiptNumber;
    @jakarta.persistence.Column(unique=true)
    private String invoiceNumber;
    @jakarta.persistence.Column(unique=true)
    private String requestKey;
    private LocalDateTime saleDate;
    private String pumpName;
    private String fuelType;
    
    private Double quantityLitres;
    private Double currentPrice;
    private Double totalAmount;
    
    private String customerName;
    private String vehicleNumber;
    private String paymentMethod;
    private String cashier;

    // Phase 4: Customer Account & E-Receipt Enhancements
    private String customerUsername;
    private String paymentStatus; // PAID, COMPLETED, PENDING, OVERDUE
    private String transactionId;
    private Double unitPrice;
    private Double discountAmount;
    private Double netAmount;
    private String notes;

    public Double getEffectiveUnitPrice() {
        if (unitPrice != null && unitPrice > 0) {
            return unitPrice;
        }
        if (currentPrice != null && currentPrice > 0) {
            return currentPrice;
        }
        if (totalAmount != null && quantityLitres != null && quantityLitres > 0) {
            return Math.round((totalAmount / quantityLitres) * 100.0) / 100.0;
        }
        return 0.0;
    }

    public String getEffectivePaymentStatus() {
        if (paymentStatus != null && !paymentStatus.trim().isEmpty()) {
            return paymentStatus.trim().toUpperCase();
        }
        return "PAID";
    }

    public String getEffectiveTransactionId() {
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            return transactionId.trim();
        }
        if (receiptNumber != null && !receiptNumber.trim().isEmpty()) {
            return "TXN-" + receiptNumber.replace("REC-", "");
        }
        return "TXN-" + (id != null ? id : "000");
    }
}
