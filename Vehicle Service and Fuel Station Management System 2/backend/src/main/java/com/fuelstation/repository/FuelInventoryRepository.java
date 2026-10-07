package com.fuelstation.repository;

import com.fuelstation.model.FuelInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelInventoryRepository extends JpaRepository<FuelInventory, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from FuelInventory e where e.id = :id")
    java.util.Optional<FuelInventory> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<FuelInventory> findByFuelType(String fuelType);
}
