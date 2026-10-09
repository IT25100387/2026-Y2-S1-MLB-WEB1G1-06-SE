package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String icon;
    @Column(length = 1000)
    private String imageUrl;
    private String category;
    private Double estimatedCost;
    private String estimatedDuration;
    
    @Column(columnDefinition = "TEXT")
    private String shortDescription;
    
    @ElementCollection
    private List<String> highlights;
    
    private boolean popular;
    private boolean active;
    
    @Transient
    private Double discountPercentage;
    
    @Transient
    private Double offerPrice;
}
