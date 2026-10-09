package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class FuelPump {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(unique=true)
    private String pumpName;
    @jakarta.persistence.Column(unique=true)
    private String pumpNumber;
    private String fuelType;
    private String location;
    private String status; // Active, Maintenance, Disabled
    private String assignedOperator;
    
    private Double currentMeterReading;
    private Double openingReading;
    private Double closingReading;
    private Double todaysSales;
    private java.time.LocalDate stationDate;
}
