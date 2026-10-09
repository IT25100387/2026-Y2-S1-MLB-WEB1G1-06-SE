package com.fuelstation.controller;

import com.fuelstation.model.AppUser;
import com.fuelstation.service.UserService;
import com.fuelstation.service.VehicleService;
import com.fuelstation.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        List<AppUser> allUsers;
        if ((search != null && !search.isBlank()) || (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) || (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))) {
            allUsers = userService.searchUsers(search, role, status);
        } else {
            allUsers = userService.getAllUsers();
        }

        List<AppUser> fullList = userService.getAllUsers();
        long activeCount = fullList.stream().filter(u -> "Active".equalsIgnoreCase(u.getStatus())).count();
        long adminCount = fullList.stream().filter(u -> "Admin".equalsIgnoreCase(u.getRole())).count();
        long staffCount = fullList.stream().filter(u -> !"Customer".equalsIgnoreCase(u.getRole()) && !"Admin".equalsIgnoreCase(u.getRole())).count();
        long customerCount = fullList.stream().filter(u -> "Customer".equalsIgnoreCase(u.getRole())).count();

        Map<String, Object> response = new HashMap<>();
        response.put("users", allUsers);
        response.put("totalCount", fullList.size());
        response.put("activeCount", activeCount);
        response.put("adminCount", adminCount);
        response.put("staffCount", staffCount);
        response.put("customerCount", customerCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        Optional<AppUser> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        int vehicleCount = 0;
        int bookingCount = 0;
        try {
            vehicleCount = vehicleService.getVehiclesByOwner(user.getUsername()).size();
            bookingCount = bookingService.getBookingsByCustomer(user.getUsername()).size();
        } catch (Exception ignored) {}

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("vehicleCount", vehicleCount);
        response.put("bookingCount", bookingCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/add")
    public ResponseEntity<?> addUser(@RequestBody AppUser user) {
        try {
            userService.createUser(user);
            return ResponseEntity.ok(Map.of("message", "User created successfully"));
        } catch (com.fuelstation.util.InputValidation.InvalidFields e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage(),"fields",e.fields()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid data"));
        }
    }

    @PostMapping("/users/update/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody AppUser formUser,
            @RequestParam(required = false) String newPassword
    ) {
        try {
            userService.updateUser(id, formUser, newPassword != null ? newPassword : formUser.getPassword());
            return ResponseEntity.ok(Map.of("message", "User updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/users/status/{id}")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(Map.of("message", "Status toggled"));
    }

    @PostMapping("/users/deactivate/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }

    @PostMapping("/users/reactivate/{id}")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User reactivated"));
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getCustomers() {
        List<AppUser> customers = userService.getCustomersOnly();
        long activeCount = customers.stream().filter(c -> "Active".equalsIgnoreCase(c.getStatus())).count();

        Map<String, Object> response = new HashMap<>();
        response.put("customers", customers);
        response.put("totalCustomers", customers.size());
        response.put("activeCount", activeCount);

        return ResponseEntity.ok(response);
    }
}
