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
public class ServiceBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String licensePlate; // References Vehicle indirectly
    private String customerName;
    private LocalDate serviceDate;
    private String timeSlot; // e.g., "09:00 AM", "11:00 AM"
    private String serviceType; // e.g., Full Service, Oil Change
    private String status; // Pending, Approved, Assigned, In Progress, Completed, Cancelled
    @jakarta.persistence.Column(length=4000)
    private String notes;
    @jakarta.persistence.Column(name="reference_number", unique=true)
    private String referenceNumber;
    @jakarta.persistence.Transient private String invoiceNumber;
    @jakarta.persistence.Transient private Double amountPaid;
    @jakarta.persistence.Transient private Boolean cancellable;
    @jakarta.persistence.Transient private Boolean editable;
    @jakarta.persistence.Version
    private Long version;

    // Phase 3 Customer Experience Extensions
    private String customerUsername;
    private String customerPhone;
    private Double estimatedCost;
    private String estimatedDuration;
    @jakarta.persistence.Column(length=4000)
    private String problemDescription;
    private String trackingStage; // Vehicle Received, Inspection, Repair / Service, Quality Check, Completed

    // POS & Invoice Customer Classification
    private String customerType; // REGISTERED_CUSTOMER, REGISTERED_VEHICLE, GUEST_CUSTOMER
}
