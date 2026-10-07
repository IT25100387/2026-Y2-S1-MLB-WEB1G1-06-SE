package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class StaffServiceImpl implements StaffService {
    @Autowired private StaffRepository staffRepository;
    @Autowired private ShiftScheduleRepository shiftRepository;
    @Autowired private AttendanceRecordRepository attendanceRepository;
    @Autowired private LeaveRequestRepository leaveRepository;
    @Autowired private JobCardRepository jobs;
    @Autowired private ServiceBookingRepository bookings;
    @Autowired private AppUserRepository accounts;
    @Autowired private NotificationService notices;
    @Autowired private Clock clock;
    private Staff staff(Long id) { return staffRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found")); }
    private boolean activeJob(JobCard j) { return !Set.of("Completed", "Cancelled").contains(j.getStatus()); }
    private void checkAssigned(Long id, LocalDate from, LocalDate to) {
        for (JobCard j : jobs.findByMechanicId(id)) if (activeJob(j)) {
            var b = bookings.findByReferenceNumber(j.getReferenceNumber());
            if (b.isPresent() && !b.get().getServiceDate().isBefore(from) && !b.get().getServiceDate().isAfter(to)) throw new IllegalStateException("Employee has assigned jobs in this period. Reassign those jobs first.");
        }
    }
    @Override @Transactional(readOnly=true) public List<Staff> getAllStaff() { return staffRepository.findAll(); }
    @Override @Transactional(readOnly=true) public Optional<Staff> getStaffById(Long id) { return staffRepository.findById(id); }
    @Override public Staff saveStaff(Staff input) {
        Staff row = input.getId() == null ? new Staff() : staff(input.getId());
        String previousRole=row.getRole();
        String previousLink = row.getSystemUsername(); String previousStatus = row.getEmploymentStatus();
        org.springframework.beans.BeanUtils.copyProperties(input, row, "id", "systemUsername", "employmentStatus");
        row.setName(Rules.required(row.getName(), "Employee name")); row.setRole(Rules.required(row.getRole(), "Job designation"));
        row.setPhone(Rules.required(row.getPhone(), "Phone")); row.setContactDetails(row.getPhone());
        row.setAssignedStation(Rules.required(row.getAssignedStation(), "Assigned station"));
        String link = input.getSystemUsername() == null ? previousLink : input.getSystemUsername().trim();
        if (link != null && !link.isBlank()) {
            AppUser account = accounts.findByUsername(link).orElseThrow(() -> new IllegalArgumentException("Linked account does not exist"));
            if (Set.of("Customer", "Admin").contains(account.getRole())) throw new IllegalArgumentException("Link an employee account");
            if (row.getRole().toLowerCase().contains("mechanic") && !"Mechanic".equalsIgnoreCase(account.getRole())) throw new IllegalArgumentException("Mechanic employees require a Mechanic account");
            if (staffRepository.findAll().stream().anyMatch(s -> !Objects.equals(s.getId(), row.getId()) && link.equals(s.getSystemUsername()))) throw new IllegalArgumentException("Account is already linked to another employee");
        }
        if ((!Objects.equals(previousLink, link)||!Objects.equals(previousRole,row.getRole())) && row.getId() != null) checkAssigned(row.getId(), LocalDate.now(clock), LocalDate.MAX);
        row.setSystemUsername(link == null || link.isBlank() ? null : link);
        String status = input.getEmploymentStatus() == null ? (previousStatus == null ? "Active" : previousStatus) : input.getEmploymentStatus();
        if (!Set.of("Active", "Inactive", "On Leave").contains(status)) throw new IllegalArgumentException("Invalid employment status");
        if (!"Active".equals(status) && row.getId() != null) checkAssigned(row.getId(), LocalDate.now(clock), LocalDate.MAX);
        row.setEmploymentStatus(status);
        Staff saved = staffRepository.save(row);
        for (ShiftSchedule s : shiftRepository.findAll()) if (saved.getId().equals(s.getStaffId())) { enrich(s, saved); shiftRepository.save(s); }
        return saved;
    }
    @Override public void deactivateStaff(Long id) { Staff s = staff(id); checkAssigned(id, LocalDate.now(clock), LocalDate.MAX); s.setEmploymentStatus("Inactive"); staffRepository.save(s); }
    @Override public void deleteStaffPermanently(Long id) {
        staff(id);
        if (!jobs.findByMechanicId(id).isEmpty() || shiftRepository.findAll().stream().anyMatch(s -> id.equals(s.getStaffId())) || attendanceRepository.findAll().stream().anyMatch(a -> id.equals(a.getStaffId())) || leaveRepository.findAll().stream().anyMatch(l -> id.equals(l.getStaffId()))) throw new IllegalStateException("Employee has linked records. Deactivate the employee to preserve history.");
        staffRepository.deleteById(id);
    }
    private void enrich(ShiftSchedule shift, Staff s) { shift.setStaffName(s.getName()); shift.setStaffRole(s.getRole()); shift.setStaffPhone(s.getPhone()); shift.setStaffEmail(s.getEmail()); if (shift.getStation() == null || shift.getStation().isBlank()) shift.setStation(s.getAssignedStation()); }
    @Override @Transactional(readOnly=true) public List<ShiftSchedule> getAllShifts() { return shiftRepository.findAll(); }
    @Override @Transactional(readOnly=true) public Optional<ShiftSchedule> getShiftById(Long id) { return shiftRepository.findById(id); }
    private long[] interval(ShiftSchedule shift) {
        var match = java.util.regex.Pattern.compile("(\\d{2}):(\\d{2}).*?(\\d{2}):(\\d{2})").matcher(shift.getShiftTiming());
        if (!match.find()) throw new IllegalArgumentException("Shift timing must include its start and end, such as Morning (08:00 - 16:00)");
        long start = shift.getShiftDate().toEpochDay()*1440 + LocalTime.of(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2))).toSecondOfDay()/60;
        long end = shift.getShiftDate().toEpochDay()*1440 + LocalTime.of(Integer.parseInt(match.group(3)), Integer.parseInt(match.group(4))).toSecondOfDay()/60;
        if (end <= start) end += 1440; return new long[]{start,end};
    }
    @Override public ShiftSchedule saveShift(ShiftSchedule input) {
        Staff s = staffRepository.lockById(input.getStaffId()).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (!"Active".equals(s.getEmploymentStatus())) throw new IllegalArgumentException("Select an active employee");
        if (input.getShiftDate() == null || input.getShiftDate().isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("Select today or a future shift date");
        Rules.required(input.getShiftTiming(), "Shift timing");
        if (leaveRepository.findAll().stream().anyMatch(l -> s.getId().equals(l.getStaffId()) && "Approved".equals(l.getStatus()) && !input.getShiftDate().isBefore(l.getStartDate()) && !input.getShiftDate().isAfter(l.getEndDate()))) throw new IllegalStateException("Employee is on approved leave");
        long[] range = interval(input);
        for (ShiftSchedule old : shiftRepository.findAll()) if (s.getId().equals(old.getStaffId()) && !Objects.equals(old.getId(), input.getId()) && !"Cancelled".equals(old.getStatus())) { long[] other = interval(old); if (range[0] < other[1] && range[1] > other[0]) throw new IllegalStateException("This shift overlaps an existing allocation"); }
        if (input.getStatus() == null) input.setStatus("Scheduled");
        if (!Set.of("Scheduled", "Active", "Completed", "Swapped", "Cancelled").contains(input.getStatus())) throw new IllegalArgumentException("Invalid shift status");
        enrich(input, s); ShiftSchedule saved = shiftRepository.save(input);
        notices.createCustomerNotification(s.getSystemUsername(), "Shift updated", input.getShiftDate()+" ? "+input.getShiftTiming(), "HR", "/dashboard/profile/shifts", "fa-calendar"); return saved;
    }
    @Override public ShiftSchedule updateShift(Long id, ShiftSchedule updated) { ShiftSchedule row = shiftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shift not found")); org.springframework.beans.BeanUtils.copyProperties(updated, row, "id"); return saveShift(row); }
    @Override public void deleteShift(Long id) { if (!shiftRepository.existsById(id)) throw new IllegalArgumentException("Shift not found"); shiftRepository.deleteById(id); }
    @Override public void updateShiftStatus(Long id, String status) { if (!Set.of("Scheduled", "Active", "Completed", "Swapped", "Cancelled").contains(status)) throw new IllegalArgumentException("Invalid shift status"); ShiftSchedule s = shiftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shift not found")); s.setStatus(status); shiftRepository.save(s); }
    @Override @Transactional(readOnly=true) public List<LeaveRequest> getAllLeaveRequests() { return leaveRepository.findAll(); }
    @Override public LeaveRequest submitLeaveRequest(LeaveRequest leave) {
        Staff s = staff(leave.getStaffId()); if (!"Active".equals(s.getEmploymentStatus())) throw new IllegalArgumentException("Select an active employee");
        if (leave.getStartDate() == null || leave.getEndDate() == null || leave.getStartDate().isBefore(LocalDate.now(clock)) || leave.getEndDate().isBefore(leave.getStartDate())) throw new IllegalArgumentException("Select a valid current or future leave period");
        if (!Set.of("Annual", "Sick", "Casual", "Emergency", "Unpaid").contains(Rules.required(leave.getLeaveType(), "Leave type"))) throw new IllegalArgumentException("Invalid leave type");
        leave.setReason(Rules.required(leave.getReason(), "Reason"));
        if (leaveRepository.findAll().stream().anyMatch(l -> s.getId().equals(l.getStaffId()) && !"Rejected".equals(l.getStatus()) && !leave.getStartDate().isAfter(l.getEndDate()) && !leave.getEndDate().isBefore(l.getStartDate()))) throw new IllegalStateException("Leave overlaps an existing request");
        leave.setId(null); leave.setStaffName(s.getName()); leave.setStatus("Pending"); LeaveRequest saved = leaveRepository.save(leave);
        notices.notifyRoles("Leave requested", s.getName()+" requested "+leave.getLeaveType()+" leave", "HR", "/dashboard/hr/leave", "MANAGER"); return saved;
    }
    @Override public LeaveRequest processLeaveAction(Long id, String action) {
        String status = switch (action.toLowerCase(java.util.Locale.ROOT)) { case "approve", "approved" -> "Approved"; case "reject", "rejected" -> "Rejected"; default -> throw new IllegalArgumentException("Choose Approve or Reject"); };
        LeaveRequest leave = leaveRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"Pending".equals(leave.getStatus())) { if (status.equals(leave.getStatus())) return leave; throw new IllegalStateException("This leave request was already processed"); }
        if ("Approved".equals(status)) checkAssigned(leave.getStaffId(), leave.getStartDate(), leave.getEndDate());
        leave.setStatus(status); LeaveRequest saved = leaveRepository.save(leave);
        notices.createCustomerNotification(staff(leave.getStaffId()).getSystemUsername(), "Leave "+status, leave.getStartDate()+" to "+leave.getEndDate(), "HR", "/dashboard/profile/leave", "fa-calendar"); return saved;
    }
    @Override @Transactional(readOnly=true) public long getPendingLeavesCount() { return leaveRepository.findAll().stream().filter(l -> "Pending".equals(l.getStatus())).count(); }
}
