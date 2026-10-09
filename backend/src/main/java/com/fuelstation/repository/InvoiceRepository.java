package com.fuelstation.repository;

import com.fuelstation.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Invoice i where i.invoiceNumber = :number")
    Optional<Invoice> lockByInvoiceNumber(@org.springframework.data.repository.query.Param("number") String number);
    List<Invoice> findByCustomerUsernameOrderByInvoiceDateDesc(String customerUsername);
    List<Invoice> findByCustomerUsernameAndStatusOrderByInvoiceDateDesc(String customerUsername, String status);
    Optional<Invoice> findByReferenceNumber(String referenceNumber);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByPurchaseRequestKey(String purchaseRequestKey);
    List<Invoice> findByStatus(String status);
}
