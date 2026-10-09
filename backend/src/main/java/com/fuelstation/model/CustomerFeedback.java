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
public class CustomerFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long jobCardId;
    private String customerName;
    private String customerUsername;
    private String serviceType;
    
    private Integer rating; // 1 to 5
    @jakarta.persistence.Column(length=2000)
    private String comments;
    private Boolean needsAttention; // True if rating < 3 or negative comments
    
    private LocalDateTime submissionDate;
}
