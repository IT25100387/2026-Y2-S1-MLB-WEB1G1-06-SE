package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity @Data
public class RefundRecord {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="payment_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private PaymentRecord payment;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceNumber;
    @jakarta.persistence.Column(name="payment_id")
    private Long paymentId;
    private Double amount;
    private Double requestedAmount;
    private String reason;
    private String recordedBy;
    private LocalDateTime createdAt;
    @Column(unique = true)
    private String requestKey;
}
