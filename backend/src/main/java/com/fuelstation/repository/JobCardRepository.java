package com.fuelstation.repository;

import com.fuelstation.model.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobCardRepository extends JpaRepository<JobCard, Long> {
    java.util.List<JobCard> findByMechanicUsername(String mechanicUsername);
    java.util.List<JobCard> findByStatusIn(java.util.Collection<String> statuses);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select j from JobCard j where j.id = :id")
    Optional<JobCard> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<JobCard> findByReferenceNumber(String referenceNumber);
    Optional<JobCard> findFirstByReferenceNumberOrderByIdDesc(String referenceNumber);
    java.util.List<JobCard> findByMechanicId(Long mechanicId);
    java.util.List<JobCard> findByBayId(Long bayId);
}
