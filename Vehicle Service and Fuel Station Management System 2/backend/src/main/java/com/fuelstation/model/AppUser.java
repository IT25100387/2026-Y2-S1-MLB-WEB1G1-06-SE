package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(unique = true)
    private String username;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String fullName;
    @jakarta.persistence.Column(unique = true)
    private String email;
    private String role; // Admin, Manager, Cashier, Mechanic, Customer
    private String status; // Active, Deactivated

    // Extended Profile & Customer Account fields
    private String phoneNumber;
    private String address;
    private String city;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    
    // Password Reset
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String resetToken;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.time.LocalDateTime resetTokenExpiry;

    private java.time.LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "Active";
        }
    }

}
