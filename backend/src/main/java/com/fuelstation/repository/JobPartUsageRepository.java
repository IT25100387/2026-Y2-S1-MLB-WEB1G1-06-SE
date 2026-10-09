package com.fuelstation.repository;

import com.fuelstation.model.JobPartUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface JobPartUsageRepository extends JpaRepository<JobPartUsage, Long> {
    List<JobPartUsage> findByJobCardIdOrderByIdAsc(Long jobCardId);
    List<JobPartUsage> findByReferenceNumber(String referenceNumber);
    Optional<JobPartUsage> findByRequestKey(String requestKey);
    boolean existsByPartId(Long partId);
}
