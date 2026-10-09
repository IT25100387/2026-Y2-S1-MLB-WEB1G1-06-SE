package com.fuelstation.controller;

import com.fuelstation.model.ShiftSchedule;
import com.fuelstation.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.regex.Pattern;

/** Retained JSON endpoints; page rendering is handled by React. */
@RestController
public class StaffController {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z. ' -]{2,60}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+94|0)?[0-9]{9,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired private StaffService staffService;

    @GetMapping("/shifts/api/feed")
    public java.util.Map<String, Object> getShiftsRealtimeFeed() {
        java.util.List<ShiftSchedule> allShifts = staffService.getAllShifts();

        long morningCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Morning")).count();
        long eveningCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Evening")).count();
        long nightCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Night")).count();

        java.util.Map<String, Object> feed = new java.util.HashMap<>();
        feed.put("morningCount", morningCount);
        feed.put("eveningCount", eveningCount);
        feed.put("nightCount", nightCount);
        feed.put("shifts", allShifts);
        return feed;
    }

    @PostMapping("/shifts/api/add")
    public org.springframework.http.ResponseEntity<?> addShiftAjax(@ModelAttribute ShiftSchedule shift) {
        if (shift.getStaffId() == null) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Please select an active employee to assign the shift."
            ));
        }

        if (shift.getShiftDate() == null) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Please specify a valid shift date."
            ));
        }

        if (shift.getShiftTiming() == null || shift.getShiftTiming().trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Please select shift timings."
            ));
        }

        if (shift.getStation() == null || shift.getStation().trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Please specify the assigned station or work area."
            ));
        }

        if (shift.getStaffName() != null && !shift.getStaffName().trim().isEmpty() && !NAME_PATTERN.matcher(shift.getStaffName().trim()).matches()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Employee name must contain only letters, dots, spaces, or hyphens."
            ));
        }

        if (shift.getStaffPhone() != null && !shift.getStaffPhone().trim().isEmpty() && !PHONE_PATTERN.matcher(shift.getStaffPhone().trim()).matches()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Contact phone number must be a valid 10-digit number."
            ));
        }

        if (shift.getStaffEmail() != null && !shift.getStaffEmail().trim().isEmpty() && !EMAIL_PATTERN.matcher(shift.getStaffEmail().trim()).matches()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Validation Error: Please enter a valid email address (e.g. employee@fuelcore.com)."
            ));
        }

        ShiftSchedule saved = staffService.saveShift(shift);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Duty roster allocated successfully for " + saved.getStaffName() + "!",
                "shift", saved
        ));
    }
}
