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
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketNumber; // e.g., TCK-4821
    private String customerUsername;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String subject;
    private String category; // Booking, FuelBilling, Technical, Warranty, General
    private String priority; // Low, Medium, High, Urgent
    @jakarta.persistence.Column(length=4000)
    private String message;
    private String status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    @jakarta.persistence.Column(length=4000)
    private String adminReply;
    private LocalDateTime createdDate;
    private LocalDateTime resolvedDate;
}
