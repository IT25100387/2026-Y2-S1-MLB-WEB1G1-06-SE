package com.fuelstation.repository;

import com.fuelstation.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByInvoiceNumberOrderByIdAsc(String invoiceNumber);
    java.util.Optional<PaymentRecord> findByRequestKey(String requestKey);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PaymentRecord p where p.id = :id")
    java.util.Optional<PaymentRecord> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    List<PaymentRecord> findByReferenceNumber(String referenceNumber);
    List<PaymentRecord> findByCustomerUsernameOrderByPaymentDateDesc(String customerUsername);
    List<PaymentRecord> findAllByOrderByPaymentDateDesc();
}
