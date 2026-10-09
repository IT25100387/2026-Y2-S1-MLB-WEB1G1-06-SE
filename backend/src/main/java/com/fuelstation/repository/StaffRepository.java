package com.fuelstation.repository;

import com.fuelstation.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Staff s where s.id = :id")
    java.util.Optional<Staff> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.Optional<Staff> findBySystemUsername(String systemUsername);
}
