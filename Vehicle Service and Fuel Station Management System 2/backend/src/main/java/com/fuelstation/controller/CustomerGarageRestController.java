package com.fuelstation.controller;

import com.fuelstation.model.AppUser;
import com.fuelstation.model.Vehicle;
import com.fuelstation.service.UserService;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customer/vehicles")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CustomerGarageRestController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;

    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getMyGarage(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        
        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(username);
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping
    public ResponseEntity<?> addVehicle(@RequestBody Vehicle vehicle, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().trim().isEmpty() ||
            vehicle.getMake() == null || vehicle.getMake().trim().isEmpty() ||
            vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing required fields."));
        }

        String cleanPlate = vehicle.getLicensePlate().trim().toUpperCase();
        if (vehicleService.getVehicleByLicensePlate(cleanPlate).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "License plate already registered."));
        }

        String fullName = username;
        String phone = "";
        Optional<AppUser> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            AppUser u = userOpt.get();
            if (u.getFullName() != null && !u.getFullName().isBlank()) fullName = u.getFullName();
            if (u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank()) phone = u.getPhoneNumber();
        }

        vehicle.setId(null);
        vehicle.setOwnerUsername(username);
        vehicle.setLicensePlate(cleanPlate);
        vehicle.setMake(vehicle.getMake().trim());
        vehicle.setModel(vehicle.getModel().trim());

        String finalOwnerName = (vehicle.getOwnerName() != null && !vehicle.getOwnerName().isBlank()) ? vehicle.getOwnerName() : fullName;
        String finalPhone = (vehicle.getOwnerContact() != null && !vehicle.getOwnerContact().isBlank()) ? vehicle.getOwnerContact() : phone;

        Vehicle saved = vehicleService.saveCustomerVehicle(vehicle, username, finalOwnerName, finalPhone);
        return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle added to garage.", "vehicle", saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleDetails(@PathVariable Long id, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleForCustomer(id, username);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("vehicle", vehicleOpt.get());
        response.put("serviceHistory", vehicleService.getServiceHistory(vehicleOpt.get().getLicensePlate()).stream().filter(b -> username.equals(b.getCustomerUsername())).toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody Vehicle formVehicle, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<Vehicle> vehicleOpt = vehicleService.getVehicleForCustomer(id, username);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied"));
        }

        if (formVehicle.getMake() == null || formVehicle.getMake().trim().isEmpty() ||
            formVehicle.getModel() == null || formVehicle.getModel().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid specs."));
        }

        Vehicle existing = vehicleOpt.get();
        if (formVehicle.getLicensePlate()!=null) existing.setLicensePlate(formVehicle.getLicensePlate());
        existing.setMake(formVehicle.getMake().trim());
        existing.setModel(formVehicle.getModel().trim());
        existing.setManufactureYear(formVehicle.getManufactureYear());
        existing.setFuelType(formVehicle.getFuelType());
        existing.setMileage(formVehicle.getMileage());
        existing.setEngineNumber(formVehicle.getEngineNumber());
        existing.setChassisNumber(formVehicle.getChassisNumber());
        existing.setOwnerName(formVehicle.getOwnerName());
        existing.setOwnerContact(formVehicle.getOwnerContact());

        Vehicle saved = vehicleService.saveVehicle(existing);
        return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle updated.", "vehicle", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        boolean deleted = vehicleService.deleteCustomerVehicle(id, username);
        if (!deleted) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied"));
        }
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle removed from garage."));
    }
}
