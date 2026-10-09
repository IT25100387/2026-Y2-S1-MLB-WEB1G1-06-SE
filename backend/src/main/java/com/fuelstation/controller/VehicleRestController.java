package com.fuelstation.controller;

import com.fuelstation.model.Vehicle;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/workshop/vehicles")
public class VehicleRestController {

    @Autowired
    private VehicleService vehicleService;

    private boolean isCustomer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_CUSTOMER".equalsIgnoreCase(ga.getAuthority()) || "Customer".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @GetMapping("/registry")
    public Map<String, Object> getAllVehicles(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("error", "Access Denied");
            return response;
        }

        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        response.put("vehicles", vehicles);
        return response;
    }

    @PostMapping("/registry/add")
    public Map<String, Object> addVehicle(@RequestBody Vehicle vehicle, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            response.put("error", "Access Denied");
            return response;
        }

        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().trim().isEmpty() ||
            vehicle.getMake() == null || vehicle.getMake().trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Missing required fields");
            return response;
        }

        vehicle.setId(null);
        vehicleService.saveVehicle(vehicle);
        response.put("success", true);
        return response;
    }

    @PostMapping("/registry/update/{id}")
    public Map<String, Object> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicleDetails, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            return response;
        }

        Optional<Vehicle> existingOpt = vehicleService.getVehicleById(id);
        if (existingOpt.isEmpty()) {
            response.put("success", false);
            response.put("error", "Vehicle not found");
            return response;
        }

        Vehicle existing = existingOpt.get();
        if (vehicleDetails.getLicensePlate() != null) existing.setLicensePlate(vehicleDetails.getLicensePlate());
        existing.setMileage(vehicleDetails.getMileage());
        existing.setMake(vehicleDetails.getMake());
        existing.setModel(vehicleDetails.getModel());
        existing.setOwnerName(vehicleDetails.getOwnerName());
        existing.setOwnerContact(vehicleDetails.getOwnerContact());
        existing.setManufactureYear(vehicleDetails.getManufactureYear());
        existing.setFuelType(vehicleDetails.getFuelType());
        existing.setEngineNumber(vehicleDetails.getEngineNumber());
        existing.setChassisNumber(vehicleDetails.getChassisNumber());
        existing.setOwnerUsername(vehicleDetails.getOwnerUsername());
        if (vehicleDetails.getRegisteredDate() != null) {
            existing.setRegisteredDate(vehicleDetails.getRegisteredDate());
        }

        vehicleService.saveVehicle(existing);
        response.put("success", true);
        return response;
    }

    @PostMapping("/registry/delete/{id}")
    public Map<String, Object> deleteVehicle(@PathVariable Long id, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            return response;
        }
        
        vehicleService.deleteVehicle(id);
        response.put("success", true);
        return response;
    }
}
