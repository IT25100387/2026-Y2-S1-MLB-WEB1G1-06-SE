package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class ShiftSchedule {
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="staff_id", referencedColumnName="id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @lombok.EqualsAndHashCode.Exclude
    private Staff employee;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Please select an employee")
    @jakarta.persistence.Column(name="staff_id")
    private Long staffId;

    @NotBlank(message = "Personal full name is required")
    @Pattern(regexp = "^[A-Za-z. ' -]+$", message = "Employee name must contain only letters, dots, spaces, or hyphens (no numbers or symbols)")
    private String staffName;
    
    private String staffRole; // e.g. Master Mechanic, Shift Supervisor

    @Pattern(regexp = "^(?:(?:\\+94|0)?[0-9]{9,10})?$", message = "Contact phone number must be valid 10 digits")
    private String staffPhone;

    private String staffEmail;
    
    @NotNull(message = "Shift date is required")
    private LocalDate shiftDate;

    @NotBlank(message = "Shift timing is required")
    private String shiftTiming; // e.g. Morning (08:00 - 16:00), Evening (16:00 - 00:00), Night (00:00 - 08:00)

    @NotBlank(message = "Assigned station / duty area is required")
    private String station; // e.g. Service Bay A, Fuel Island 1, POS Cash Counter

    private String status = "Scheduled"; // Scheduled, Active, Completed, Swapped, Cancelled

    private String notes; // Shift assignment notes / special instructions
}
