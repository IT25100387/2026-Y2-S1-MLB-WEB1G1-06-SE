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
public class FuelInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @jakarta.persistence.Version
    private Long version;
    
    @jakarta.persistence.Column(unique=true)
    private String fuelType; // Petrol 92, Petrol 95, Auto Diesel, Super Diesel, Kerosene
    private Double currentStockLitres;
    private Double minStockWarning;
    private Double maxCapacityLitres;
    
    private String supplier;
    private LocalDateTime lastDeliveryDate;
    private String status; // Normal, Low Stock, Critical
}
