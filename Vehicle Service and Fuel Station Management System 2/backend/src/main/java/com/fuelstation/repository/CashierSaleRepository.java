package com.fuelstation.repository;

import com.fuelstation.model.CashierSale;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CashierSaleRepository extends JpaRepository<CashierSale,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CashierSale s where s.id = :id")
    Optional<CashierSale> lockById(@Param("id") String id);
    List<CashierSale> findByReferenceNumber(String referenceNumber);
    List<CashierSale> findByState(String state);
}
