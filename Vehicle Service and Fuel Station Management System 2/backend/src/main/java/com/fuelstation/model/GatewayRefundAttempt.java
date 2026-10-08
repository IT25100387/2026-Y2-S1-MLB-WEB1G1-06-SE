package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Committed independently of the ledger so an uncertain refund is never sent twice. */
@Entity @Data
public class GatewayRefundAttempt {
    @Id private String id;
    private Long paymentId;
    private String gatewayPaymentId;
    private Double amount;
    @Column(length=255) private String reason;
    private String state;
    private String gatewayRefundId;
    private LocalDateTime createdAt;
    @Version private Long version;
}
