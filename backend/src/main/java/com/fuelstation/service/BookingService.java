package com.fuelstation.service;

import com.fuelstation.model.ServiceBooking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingService {
    List<ServiceBooking> getAllBookings();
    Optional<ServiceBooking> getBookingById(Long id);
    Optional<ServiceBooking> getBookingByReferenceNumber(String referenceNumber);
    boolean isSlotAvailable(LocalDate serviceDate, String timeSlot);
    ServiceBooking createBooking(ServiceBooking booking);
    void cancelBooking(String referenceNumber);
    void deleteBooking(String referenceNumber);
    long getPendingBookingsCount();
    long getCompletedBookingsCount();
    List<ServiceBooking> getPendingBookings();

    // Phase 3 Customer Methods
    List<ServiceBooking> getBookingsByCustomer(String username);
    Optional<ServiceBooking> getBookingForCustomer(String referenceNumber, String username);
    boolean isBookingOwnedByUser(String referenceNumber, String username);
    boolean isSlotAvailableForReschedule(LocalDate serviceDate, String timeSlot, String referenceNumber);
    ServiceBooking rescheduleBooking(String referenceNumber, LocalDate newDate, String newSlot, String username);
    void cancelBookingForCustomer(String referenceNumber, String username);
    ServiceBooking updateBookingStatus(String referenceNumber, String status);
    ServiceBooking updateBookingAdmin(String referenceNumber, java.time.LocalDate serviceDate, String timeSlot, String serviceType);
    ServiceBooking updateBookingAdmin(String referenceNumber, java.time.LocalDate serviceDate, String timeSlot, String serviceType, String notes);

    // POS Cashier Methods
    List<ServiceBooking> searchBookingsForPOS(String query);
    String deriveCustomerType(ServiceBooking booking);
}
