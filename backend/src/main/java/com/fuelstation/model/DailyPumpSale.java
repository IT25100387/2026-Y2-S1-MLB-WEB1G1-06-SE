package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@jakarta.persistence.Table(uniqueConstraints=@jakarta.persistence.UniqueConstraint(columnNames={"pump_id", "sale_date"}))
@Data
public class DailyPumpSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private FuelPump pump;
    
    private LocalDate saleDate;
    
    private Double litersSold;
    private Double unitPrice;
    private Double expectedAmount;
    private Double collectedAmount;
    private Double difference;
    
    private String pumpStatus;
    private String deactivationReason;
    
    private Boolean confirmed = false;
    private String recordedBy;
    private java.time.LocalDateTime confirmedAt;
    @jakarta.persistence.Version private Long version;
}
