package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class LeaveRequest {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="staff_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private Staff employee;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @jakarta.persistence.Column(name="staff_id")
    private Long staffId;
    private String staffName;
    
    private String leaveType; // Annual, Sick, Casual
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    
    private String status; // Pending, Approved, Rejected
}
