package com.fuelstation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Employee full legal name is required")
    @Size(min = 2, max = 60, message = "Name must be between 2 and 60 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .\\u2019' -]+$", message = "Name must contain only alphabetic letters, spaces, dots, and hyphens (no numbers or special symbols)")
    private String name;

    @NotBlank(message = "Job designation is required")
    private String role; // e.g. Station Manager, HR Officer, Mechanic, Attendant, Cashier

    @NotBlank(message = "Phone number is required")
    @Size(max = 25, message = "Phone allows at most 25 characters")
    private String phone;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email format (e.g. employee@fuelcore.com)")
    private String email;

    // Retained for backward compatibility with existing views and records
    private String contactDetails;

    @NotBlank(message = "Assigned station is required")
    private String assignedStation; // e.g. Service Bay A, Fuel Pump 1, POS Cash Counter

    private String employmentStatus = "Active"; // Active, On Leave, Inactive

    private String systemUsername; // To link to AppUser account

    // Helper method ensuring contactDetails syncs with phone
    public String getContactDetails() {
        if (contactDetails != null && !contactDetails.trim().isEmpty()) {
            return contactDetails;
        }
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        if (this.contactDetails == null || this.contactDetails.trim().isEmpty()) {
            this.contactDetails = phone;
        }
    }
}
