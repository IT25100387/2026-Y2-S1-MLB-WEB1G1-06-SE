package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class JobCard {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="reference_number", referencedColumnName="reference_number", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private ServiceBooking booking;

    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="mechanic_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private Staff mechanic;

    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="bay_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private ServiceBay bay;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(name="reference_number", unique=true)
    private String referenceNumber;
    private String licensePlate;
    private String customerName;
    
    @jakarta.persistence.Column(name="mechanic_id")
    private Long mechanicId;
    private String mechanicName;
    private String mechanicUsername;
    
    @jakarta.persistence.Column(name="bay_id")
    private Long bayId;
    private String bayName;
    
    private String serviceType;
    private String status; // Pending, In Progress, Waiting for Parts, Quality Check, Completed
    
    @jakarta.persistence.Column(length = 4000)
    private String mechanicNotes;
    @jakarta.persistence.Column(length = 4000)
    private String partsUsed;
    @jakarta.persistence.Version
    private Long version;
}
