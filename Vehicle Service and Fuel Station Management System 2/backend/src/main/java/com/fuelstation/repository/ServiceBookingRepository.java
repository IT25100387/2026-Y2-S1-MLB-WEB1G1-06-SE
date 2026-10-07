package com.fuelstation.repository;

import com.fuelstation.model.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    java.util.Optional<ServiceBooking> findByReferenceNumber(String referenceNumber);
    boolean existsByServiceDateAndTimeSlotAndStatusNot(LocalDate serviceDate, String timeSlot, String status);
    boolean existsByServiceDateAndTimeSlotAndStatusNotAndIdNot(LocalDate serviceDate, String timeSlot, String status, Long id);
    List<ServiceBooking> findByLicensePlateOrderByServiceDateDesc(String licensePlate);
    List<ServiceBooking> findByCustomerUsernameOrderByServiceDateDesc(String customerUsername);
    List<ServiceBooking> findByCustomerUsernameOrCustomerNameIgnoreCaseOrderByServiceDateDesc(String customerUsername, String customerName);
    List<ServiceBooking> findByLicensePlateContainingIgnoreCaseOrderByServiceDateDesc(String licensePlate);
    List<ServiceBooking> findByReferenceNumberIn(List<String> refNumbers);
}
