package com.fuelstation.repository;

import com.fuelstation.model.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {
    Optional<RefundRecord> findByRequestKey(String requestKey);
    List<RefundRecord> findByInvoiceNumberOrderByIdAsc(String invoiceNumber);
}
