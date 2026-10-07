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
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    @jakarta.persistence.Column(length = 5000)
    private String message;
    private String type; // e.g., Booking, Alert, Report
    private String status; // Unread, Read
    private LocalDateTime createdDate;

    // Every role receives personal rows with independent read state.
    private String recipientUsername;
    private String actionUrl;
    private String icon;
}
