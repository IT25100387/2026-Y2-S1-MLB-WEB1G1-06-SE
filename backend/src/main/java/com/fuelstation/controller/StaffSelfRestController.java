package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr")
public class StaffSelfRestController {
    @Autowired private StaffRepository employees;
    @Autowired private AppUserRepository accounts;
    @Autowired private StaffService hr;
    private Staff own(Authentication auth) { return employees.findBySystemUsername(auth.getName()).orElseThrow(() -> new IllegalStateException("Ask your manager to link your employee record to this account")); }
    @GetMapping("/employee-accounts")
    public Object accountChoices() { return accounts.findAll().stream().filter(u -> java.util.Set.of("Manager", "Cashier", "Mechanic").contains(u.getRole())).map(u -> Map.of("username", u.getUsername(), "fullName", u.getFullName(), "role", u.getRole())).toList(); }
    @GetMapping("/self")
    public Object workspace(Authentication auth) { Staff s = own(auth); return Map.of("staff", s, "shifts", hr.getAllShifts().stream().filter(a -> s.getId().equals(a.getStaffId())).toList(), "leave", hr.getAllLeaveRequests().stream().filter(a -> s.getId().equals(a.getStaffId())).toList()); }
    @PostMapping("/self/leave") public Object leave(@RequestBody LeaveRequest input, Authentication auth) { input.setStaffId(own(auth).getId()); return Map.of("success", true, "leave", hr.submitLeaveRequest(input)); }
}
