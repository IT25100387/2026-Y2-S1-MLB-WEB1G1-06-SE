package com.fuelstation.repository;

import com.fuelstation.model.DailyPumpSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPumpSaleRepository extends JpaRepository<DailyPumpSale, Long> {
    Optional<DailyPumpSale> findByPumpIdAndSaleDate(Long pumpId, LocalDate saleDate);
    List<DailyPumpSale> findBySaleDateGreaterThanEqualOrderBySaleDateDesc(LocalDate date);
    List<DailyPumpSale> findAllByOrderBySaleDateDesc();
}
