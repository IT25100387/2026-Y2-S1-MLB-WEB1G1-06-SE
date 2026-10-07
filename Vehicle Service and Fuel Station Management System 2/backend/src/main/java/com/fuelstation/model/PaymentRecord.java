package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class PaymentRecord {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="invoice_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private Invoice invoice;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String referenceNumber;
    private String invoiceNumber;
    @jakarta.persistence.Column(name="invoice_id")
    private Long invoiceId;
    @jakarta.persistence.Column(unique = true)
    private String requestKey;
    private Double refundedAmount;
    
    private LocalDate paymentDate;
    private String paidOccupant;
    private String paymentMethod; // Cash, Cheque, Bank Deposit, Other, Credit Card, Wallet
    private Double amount;
    
    private String customerUsername;
    private String status; // SUCCESS, PENDING, FAILED, REFUNDED
    private String notes;
    @jakarta.persistence.Version
    private Long version;
}
