package com.fuelstation.repository;

import com.fuelstation.model.ServiceBay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from ServiceBay b order by b.id")
    java.util.List<ServiceBay> lockForScheduling();
}
