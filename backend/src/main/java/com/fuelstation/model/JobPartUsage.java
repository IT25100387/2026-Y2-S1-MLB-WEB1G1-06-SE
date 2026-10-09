package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class JobPartUsage {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="job_card_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private JobCard job;

    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="part_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private SparePart part;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @jakarta.persistence.Column(name="job_card_id")
    private Long jobCardId;
    private String referenceNumber;
    @jakarta.persistence.Column(name="part_id")
    private Long partId;
    private String partName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalAmount;
    private String recordedBy;
    private LocalDateTime recordedAt;
    @Column(unique = true)
    private String requestKey;
}
