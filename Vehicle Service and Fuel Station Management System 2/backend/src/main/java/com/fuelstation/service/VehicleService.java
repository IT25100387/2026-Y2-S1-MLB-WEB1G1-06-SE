package com.fuelstation.service;

import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.Vehicle;
import java.util.List;
import java.util.Optional;

public interface VehicleService {
    List<Vehicle> getAllVehicles();
    Optional<Vehicle> getVehicleById(Long id);
    Optional<Vehicle> getVehicleByLicensePlate(String licensePlate);
    Vehicle saveVehicle(Vehicle vehicle);
    void deleteVehicle(Long id);
    List<ServiceBooking> getServiceHistory(String licensePlate);

    // Phase 2: Customer Vehicle Management (My Garage)
    List<Vehicle> getVehiclesByOwner(String username);
    Optional<Vehicle> getVehicleForCustomer(Long id, String username);
    Vehicle saveCustomerVehicle(Vehicle vehicle, String username, String ownerName, String ownerContact);
    boolean deleteCustomerVehicle(Long id, String username);
    boolean isVehicleOwnedByUser(Long id, String username);
}
