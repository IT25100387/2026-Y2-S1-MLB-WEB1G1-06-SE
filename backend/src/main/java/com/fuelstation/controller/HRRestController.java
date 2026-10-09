package com.fuelstation.controller;

import com.fuelstation.model.LeaveRequest;
import com.fuelstation.model.ShiftSchedule;
import com.fuelstation.model.Staff;
import com.fuelstation.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/hr")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class HRRestController {


    @Autowired
    private StaffService staffService;

    // ==========================================
    // 1. STAFF DIRECTORY
    // ==========================================

    @GetMapping("/staff")
    public ResponseEntity<List<Staff>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @PostMapping("/staff/add")
    public ResponseEntity<?> addStaff(@RequestBody Staff staff) {
        String error = validateStaffDetails(staff.getName(), staff.getPhone(), staff.getEmail(), staff.getRole(), staff.getAssignedStation());
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", error));
        }

        staff.setId(null);
        staff.setContactDetails(staff.getPhone());
        Staff saved = staffService.saveStaff(staff);
        return ResponseEntity.ok(Map.of("success", true, "message", "Employee registered successfully", "staff", saved));
    }

    @PutMapping("/staff/update/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @RequestBody Staff staff) {
        String error = validateStaffDetails(staff.getName(), staff.getPhone(), staff.getEmail(), staff.getRole(), staff.getAssignedStation());
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", error));
        }

        staff.setId(id);
        staff.setContactDetails(staff.getPhone());
        Staff saved = staffService.saveStaff(staff);
        return ResponseEntity.ok(Map.of("success", true, "message", "Employee updated successfully", "staff", saved));
    }

    @PutMapping("/staff/status/{id}")
    public ResponseEntity<?> toggleStaffStatus(@PathVariable Long id) {
        Staff employee = staffService.getStaffById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        employee.setEmploymentStatus("Inactive".equals(employee.getEmploymentStatus()) ? "Active" : "Inactive");
        staffService.saveStaff(employee);
        return ResponseEntity.ok(Map.of("success", true, "message", "Employee status updated"));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaffPermanently(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Employee removed permanently"));
    }

    // ==========================================
    // 2. SHIFT SCHEDULES
    // ==========================================

    @GetMapping("/shifts")
    public ResponseEntity<Map<String, Object>> getShiftsFeed() {
        List<ShiftSchedule> allShifts = staffService.getAllShifts();

        long morningCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Morning")).count();
        long eveningCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Evening")).count();
        long nightCount = allShifts.stream().filter(s -> s.getShiftTiming() != null && s.getShiftTiming().contains("Night")).count();

        Map<String, Object> feed = new HashMap<>();
        feed.put("morningCount", morningCount);
        feed.put("eveningCount", eveningCount);
        feed.put("nightCount", nightCount);
        feed.put("shifts", allShifts);
        return ResponseEntity.ok(feed);
    }

    @PostMapping("/shifts/add")
    public ResponseEntity<?> addShift(@RequestBody ShiftSchedule shift) {
        if (shift.getStaffId() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please select an active employee."));
        if (shift.getShiftDate() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Specify a valid shift date."));
        if (shift.getShiftTiming() == null || shift.getShiftTiming().trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Select shift timings."));
        
        ShiftSchedule saved = staffService.saveShift(shift);
        return ResponseEntity.ok(Map.of("success", true, "message", "Shift allocated successfully!", "shift", saved));
    }

    @PutMapping("/shifts/update/{id}")
    public ResponseEntity<?> updateShift(@PathVariable Long id, @RequestBody ShiftSchedule shift) {
        if (shift.getShiftDate() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Shift date cannot be empty."));
        
        staffService.updateShift(id, shift);
        return ResponseEntity.ok(Map.of("success", true, "message", "Shift updated successfully!"));
    }

    @PutMapping("/shifts/status/{id}")
    public ResponseEntity<?> updateShiftStatus(@PathVariable Long id, @RequestParam String status) {
        staffService.updateShiftStatus(id, status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Shift status updated to " + status));
    }

    @DeleteMapping("/shifts/{id}")
    public ResponseEntity<?> deleteShift(@PathVariable Long id) {
        staffService.deleteShift(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Shift removed"));
    }

    // ==========================================
    // 3. LEAVE REQUESTS
    // ==========================================

    @GetMapping("/leave")
    public ResponseEntity<List<LeaveRequest>> getAllLeaves() {
        return ResponseEntity.ok(staffService.getAllLeaveRequests());
    }

    @PostMapping("/leave/add")
    public ResponseEntity<?> addLeave(@RequestBody LeaveRequest request) {
        if (request.getStaffId() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Select staff."));
        if (request.getStartDate() == null || request.getEndDate() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Select dates."));
        
        staffService.submitLeaveRequest(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Leave request submitted successfully."));
    }

    @PutMapping("/leave/action/{id}")
    public ResponseEntity<?> leaveAction(@PathVariable Long id, @RequestParam String action) {
        staffService.processLeaveAction(id, action);
        return ResponseEntity.ok(Map.of("success", true, "message", "Leave request " + action.toLowerCase() + "d."));
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private String validateStaffDetails(String name, String phone, String email, String role, String station) {
        try {
            com.fuelstation.util.InputValidation.name(name,"Staff name",true);
            com.fuelstation.util.InputValidation.text(name,"Staff name",60,true,false);
            com.fuelstation.util.InputValidation.phone(phone,"Phone",true);
            com.fuelstation.util.InputValidation.email(email,true);
            com.fuelstation.util.InputValidation.text(role,"Designation",100,true,false);
            com.fuelstation.util.InputValidation.text(station,"Station",100,true,false);
            return null;
        } catch (IllegalArgumentException error) { return error.getMessage(); }
    }
}
