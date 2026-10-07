package com.fuelstation.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String username;

    private String userRole;

    @Column(length = 1000)
    private String details;

    private String ipAddress;

    private String status;

    private LocalDateTime timestamp;
}
