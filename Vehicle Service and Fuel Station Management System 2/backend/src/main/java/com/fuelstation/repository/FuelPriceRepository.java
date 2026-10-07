package com.fuelstation.repository;

import com.fuelstation.model.FuelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    Optional<FuelPrice> findByFuelType(String fuelType);
    Optional<FuelPrice> findByFuelTypeIgnoreCase(String fuelType);
}
