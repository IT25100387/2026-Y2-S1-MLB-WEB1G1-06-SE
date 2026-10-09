package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class SparePart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String partName;
    private String category; // Engine Parts, Filters, Brake Parts, Tyres, etc.
    private Integer stockQuantity;
    private Integer minStockWarning;
    
    private Double buyingPrice;
    private Double sellingPrice;
    
    private String supplier;
    @jakarta.persistence.Column(length = 1000)
    private String imageUrl;
    private String status;
    @jakarta.persistence.Version
    private Long version;
}
