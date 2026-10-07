package com.fuelstation.service;

import com.fuelstation.model.LeaveRequest;
import com.fuelstation.model.ShiftSchedule;
import com.fuelstation.model.Staff;

import java.util.List;
import java.util.Optional;

public interface StaffService {
    // Staff
    List<Staff> getAllStaff();
    Optional<Staff> getStaffById(Long id);
    Staff saveStaff(Staff staff);
    void deactivateStaff(Long id);
    void deleteStaffPermanently(Long id);

    // Shifts
    List<ShiftSchedule> getAllShifts();
    Optional<ShiftSchedule> getShiftById(Long id);
    ShiftSchedule saveShift(ShiftSchedule shift);
    ShiftSchedule updateShift(Long id, ShiftSchedule shift);
    void deleteShift(Long id);
    void updateShiftStatus(Long id, String status);

    // Leave
    List<LeaveRequest> getAllLeaveRequests();
    LeaveRequest submitLeaveRequest(LeaveRequest leave);
    LeaveRequest processLeaveAction(Long id, String action);
    long getPendingLeavesCount();
}
