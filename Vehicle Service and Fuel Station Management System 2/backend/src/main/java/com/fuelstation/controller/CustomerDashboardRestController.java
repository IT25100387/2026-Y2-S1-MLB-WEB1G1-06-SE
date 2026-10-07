package com.fuelstation.controller;

import com.fuelstation.model.AppUser;
import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.Vehicle;
import com.fuelstation.service.BookingService;
import com.fuelstation.service.UserService;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customer/dashboard")
public class CustomerDashboardRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<AppUser> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(username);
        List<ServiceBooking> bookings = bookingService.getBookingsByCustomer(username);
        
        long pendingBookings = bookings.stream().filter(b -> b.getStatus().equals("Pending")).count();
        long completedBookings = bookings.stream().filter(b -> b.getStatus().equals("Completed") || b.getStatus().equals("Paid")).count();

        Map<String, Object> response = new HashMap<>();
        response.put("fullName", userOpt.get().getFullName());
        response.put("vehicleCount", vehicles.size());
        response.put("bookingCount", bookings.size());
        response.put("pendingBookings", pendingBookings);
        response.put("completedBookings", completedBookings);
        response.put("recentVehicles", vehicles.stream().limit(3).toList());
        response.put("recentBookings", bookings.stream()
                .sorted((a,b) -> b.getServiceDate().compareTo(a.getServiceDate()))
                .limit(3)
                .toList());

        return ResponseEntity.ok(response);
    }
}
