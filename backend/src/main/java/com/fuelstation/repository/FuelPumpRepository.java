package com.fuelstation.repository;

import com.fuelstation.model.FuelPump;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuelPumpRepository extends JpaRepository<FuelPump, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from FuelPump e where e.id = :id")
    java.util.Optional<FuelPump> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
