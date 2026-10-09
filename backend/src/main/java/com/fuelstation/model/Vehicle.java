package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(unique=true)
    private String licensePlate;
    private String make;
    private String model;
    private String ownerName;
    private String ownerContact;
    private String registeredDate;

    // Phase 2: Extended Vehicle Specifications & Ownership
    private Integer manufactureYear;
    private String fuelType;
    private Integer mileage;
    private String engineNumber;
    private String chassisNumber;
    private String ownerUsername;
}
