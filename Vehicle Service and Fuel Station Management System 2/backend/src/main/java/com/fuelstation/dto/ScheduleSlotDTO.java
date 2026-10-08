package com.fuelstation.dto;

import lombok.Data;

// Test comment 2

@Data
public class ScheduleSlotDTO {
    private String timeSlot;
    private boolean assigned;
    private String licensePlate;
    private String allocatedBay;
    private String mechanicName;
    private String serviceType;

    public ScheduleSlotDTO(String timeSlot) {
        this.timeSlot = timeSlot;
        this.assigned = false;
    }
}
