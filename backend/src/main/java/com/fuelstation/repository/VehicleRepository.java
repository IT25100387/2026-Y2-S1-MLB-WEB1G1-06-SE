package com.fuelstation.repository;

import com.fuelstation.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    Optional<Vehicle> findFirstByLicensePlateIgnoreCase(String licensePlate);
    List<Vehicle> findByOwnerUsername(String ownerUsername);
    List<Vehicle> findByOwnerUsernameIgnoreCase(String ownerUsername);
    List<Vehicle> findByOwnerUsernameOrOwnerNameIgnoreCase(String ownerUsername, String ownerName);
}
