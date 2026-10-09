package com.fuelstation.service;

import com.fuelstation.model.Invoice;
import com.fuelstation.model.PaymentRecord;
import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.SparePart;
import com.fuelstation.model.Supplier;

import java.util.List;
import java.util.Optional;

public interface BillingService {
    // Invoices
    List<Invoice> getAllInvoices();
    Optional<Invoice> getInvoiceById(Long id);
    Invoice saveInvoice(Invoice invoice);
    Invoice createAdvanceInvoice(ServiceBooking booking, double amount);
    Invoice updateServiceInvoice(ServiceBooking booking);
    void closeExpiredServiceInvoice(ServiceBooking booking);
    Invoice checkoutService(String referenceNumber, Double discount, String method, Double amount, Double cashTendered, String requestKey);
    Invoice purchasePart(Long partId, Integer quantity, String customerUsername, String licensePlate, String customerName, String method, Double cashTendered, String requestKey);
    Invoice createSparePartInvoice(SparePart part, Integer quantity, String customerUsername, String paymentMethod, Double cashTendered);
    List<Invoice> getInvoicesByCustomer(String customerUsername);
    Optional<Invoice> getInvoiceForCustomer(String invoiceNumber, String customerUsername);

    // Payments
    PaymentRecord processPayment(PaymentRecord payment);
    PaymentRecord processGatewayPayment(PaymentRecord payment, String gatewayPaymentId);
    List<PaymentRecord> getPaymentsByReferenceNumber(String referenceNumber);
    List<PaymentRecord> getPaymentsByCustomer(String customerUsername);
    List<PaymentRecord> getAllPayments();
    PaymentRecord processCustomerPayment(String invoiceNumber, String customerUsername, String paymentMethod, Double amount);
    PaymentRecord processCustomerPayment(String invoiceNumber, String customerUsername, String paymentMethod, Double amount, String requestKey);
    boolean refundPayment(Long paymentId, String reason);
    boolean refundPayment(Long paymentId, Double amount, String reason, String requestKey);
    boolean refundPayment(Long paymentId, Double amount, String reason, String requestKey, String method);
    boolean refundInvoice(String invoiceNumber, Double refundingAmount, String reason);
    boolean refundInvoice(String invoiceNumber, Double refundingAmount, String reason, String requestKey);
    boolean refundInvoice(String invoiceNumber, Double refundingAmount, String reason, String requestKey, String method);
    PaymentRecord updatePaymentStatus(Long paymentId, String status);

    // Suppliers
    List<Supplier> getAllSuppliers();
    Optional<Supplier> getSupplierById(Long id);
    Supplier saveSupplier(Supplier supplier);
    void deleteSupplier(Long id);

    // Spare Parts
    List<SparePart> getAllSpareParts();
    Optional<SparePart> getSparePartById(Long id);
    SparePart saveSparePart(SparePart part);
    void deleteSparePart(Long id);
}
