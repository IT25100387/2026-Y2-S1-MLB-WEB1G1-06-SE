package com.fuelstation.repository;

import com.fuelstation.model.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from SparePart p where p.id = :id")
    java.util.Optional<SparePart> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
