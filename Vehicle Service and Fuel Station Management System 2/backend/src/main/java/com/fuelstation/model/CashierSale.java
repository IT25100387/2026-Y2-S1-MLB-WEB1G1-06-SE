package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cashier checkout/audit record. Card data and merchant secrets are never stored here. */
@Entity
@Data
public class CashierSale {
    @Id private String id;
    private String requestFingerprint;
    private String recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime expiresAt;
    private String referenceNumber;
    private String paymentMethod;
    private String state;
    private Long invoiceId;
    private Double paymentAmount;
    private Double invoicePaidBefore;
    private Double offerSavings;
    private Double changeDue;
    private String gatewayPaymentId;
    private Boolean settlementBlocked;
    @Column(length=2000) private String message;
    @Version private Long version;
    @ElementCollection(fetch=FetchType.EAGER)
    private List<CashierSaleLine> lines = new ArrayList<>();
}
