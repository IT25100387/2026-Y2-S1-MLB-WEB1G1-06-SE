package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    @Autowired private ServiceBookingRepository bookingRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JobCardRepository jobRepository;
    @Autowired private ServiceBayRepository bayRepository;
    @Autowired private ServiceCatalogService catalog;
    @Autowired private OfferService offers;
    @Autowired private BillingService billing;
    @Autowired private WorkshopService workshop;
    @Autowired private SchedulingService scheduling;
    @Autowired private Clock clock;

    @Override @Transactional(readOnly = true)
    public List<ServiceBooking> getAllBookings() { return bookingRepository.findAll(); }
    @Override @Transactional(readOnly = true)
    public Optional<ServiceBooking> getBookingById(Long id) { return bookingRepository.findById(id); }
    @Override @Transactional(readOnly = true)
    public Optional<ServiceBooking> getBookingByReferenceNumber(String ref) { return ref == null ? Optional.empty() : bookingRepository.findByReferenceNumber(ref); }
    @Override @Transactional(readOnly = true)
    public boolean isSlotAvailable(LocalDate date, String slot) { return scheduling.hasCapacity(date, slot, "1 Hour", null); }
    private void populateContext(ServiceBooking b) {
        b.setLicensePlate(Rules.required(b.getLicensePlate(), "Vehicle license plate").toUpperCase(Locale.ROOT));
        Vehicle vehicle = vehicleRepository.findByLicensePlate(b.getLicensePlate()).orElse(null);
        if (vehicle != null && vehicle.getOwnerUsername() != null && !vehicle.getOwnerUsername().isBlank()) b.setCustomerUsername(vehicle.getOwnerUsername());
        if (b.getCustomerUsername() != null && !b.getCustomerUsername().isBlank()) {
            AppUser customer = appUserRepository.findByUsernameIgnoreCase(b.getCustomerUsername()).orElseThrow(() -> new IllegalArgumentException("Customer account not found"));
            if (!"Customer".equalsIgnoreCase(customer.getRole()) && !"ROLE_CUSTOMER".equalsIgnoreCase(customer.getRole())) throw new IllegalArgumentException("Select a customer account");
            b.setCustomerUsername(customer.getUsername()); b.setCustomerName(customer.getFullName()); b.setCustomerPhone(customer.getPhoneNumber()); b.setCustomerType("REGISTERED_CUSTOMER");
        } else if (vehicle != null) {
            b.setCustomerUsername(null); b.setCustomerName(vehicle.getOwnerName()); b.setCustomerPhone(vehicle.getOwnerContact()); b.setCustomerType("REGISTERED_VEHICLE");
        } else {
            b.setCustomerUsername(null); b.setCustomerName(Rules.required(b.getCustomerName(), "Guest customer name")); b.setCustomerPhone(Rules.required(b.getCustomerPhone(), "Customer contact")); b.setCustomerType("GUEST_CUSTOMER");
        }
    }
    private void populatePackage(ServiceBooking b) {
        ServiceCatalogItem service = catalog.getServiceByName(Rules.required(b.getServiceType(), "Service type")).filter(ServiceCatalogItem::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Select an active service package"));
        b.setServiceType(service.getName()); b.setEstimatedDuration(service.getEstimatedDuration());
        double price = Rules.nonnegative(service.getEstimatedCost(), "Service price");
        for (Offer offer : offers.getActiveOffersByType(OfferTargetType.SERVICE)) if (Objects.equals(offer.getTargetId(), service.getId())) {
            price = Rules.money(price * (1 - offer.getDiscountPercentage() / 100)); break;
        }
        b.setEstimatedCost(price);
    }
    private void validateNotes(String notes) { if (notes!=null && notes.length()>4000) throw new IllegalArgumentException("Booking notes must be at most 4000 characters"); }
    @Override
    public ServiceBooking createBooking(ServiceBooking booking) {
        scheduling.lockResources();
        booking.setId(null); booking.setVersion(null);
        booking.setReferenceNumber("REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        populateContext(booking); populatePackage(booking);
        scheduling.validateInterval(booking.getServiceDate(), booking.getTimeSlot(), booking.getEstimatedDuration());
        if (!scheduling.hasCapacity(booking.getServiceDate(), booking.getTimeSlot(), booking.getEstimatedDuration(), null)) throw new IllegalStateException("No bay/mechanic capacity remains for the service duration");
        booking.setStatus("Pending"); booking.setTrackingStage("Booking Confirmed");
        if (booking.getProblemDescription() != null && !booking.getProblemDescription().isBlank()) booking.setNotes(booking.getProblemDescription().trim());
        validateNotes(booking.getNotes()); validateNotes(booking.getProblemDescription());
        ServiceBooking saved = bookingRepository.save(booking);
        billing.updateServiceInvoice(saved);
        notificationService.bookingChanged(saved, "Service booked");
        return saved;
    }
    @Override
    public void cancelBooking(String ref) {
        scheduling.lockResources();
        ServiceBooking b = bookingRepository.findByReferenceNumber(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!"Pending".equals(b.getStatus()) || jobRepository.findByReferenceNumber(ref).isPresent()) throw new IllegalStateException("Only unpaid Pending bookings can be cancelled");
        Invoice inv = invoiceRepository.findByReferenceNumber(ref).orElse(null);
        if (inv != null) {
            inv = invoiceRepository.lockByInvoiceNumber(inv.getInvoiceNumber()).orElseThrow();
            if (Rules.amount(inv.getAmountPaid()) > 0 || Rules.amount(inv.getRefundedAmount()) > 0) throw new IllegalStateException("Bookings with a payment cannot be cancelled");
            inv.setStatus("VOID"); invoiceRepository.save(inv); notificationService.invoiceChanged(inv, "Unpaid service invoice voided");
        }
        b.setStatus("Cancelled"); b.setTrackingStage("Cancelled"); bookingRepository.save(b); notificationService.bookingChanged(b, "Booking cancelled");
    }
    @Override
    public void deleteBooking(String ref) {
        ServiceBooking b = bookingRepository.findByReferenceNumber(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (invoiceRepository.findByReferenceNumber(ref).isPresent() || jobRepository.findByReferenceNumber(ref).isPresent()) {
            if ("Cancelled".equals(b.getStatus())) throw new IllegalStateException("This cancelled booking is retained with its invoice history");
            cancelBooking(ref); return;
        }
        if (!"Pending".equals(b.getStatus())) throw new IllegalStateException("Completed/allocated service history cannot be deleted");
        bookingRepository.delete(b);
    }
    @Override @Transactional(readOnly = true)
    public long getPendingBookingsCount() { return getAllBookings().stream().filter(b -> "Pending".equals(b.getStatus())).count(); }
    @Override @Transactional(readOnly = true)
    public long getCompletedBookingsCount() { return getAllBookings().stream().filter(b -> "Completed".equals(b.getStatus())).count(); }
    @Override @Transactional(readOnly = true)
    public List<ServiceBooking> getPendingBookings() {
        return getAllBookings().stream().filter(b -> ("Pending".equals(b.getStatus()) || "Approved".equals(b.getStatus())) && jobRepository.findByReferenceNumber(b.getReferenceNumber()).isEmpty()).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<ServiceBooking> getBookingsByCustomer(String username) { return username == null ? List.of() : bookingRepository.findByCustomerUsernameOrderByServiceDateDesc(username); }
    @Override @Transactional(readOnly = true)
    public Optional<ServiceBooking> getBookingForCustomer(String ref, String username) { return getBookingByReferenceNumber(ref).filter(b -> username != null && username.equalsIgnoreCase(b.getCustomerUsername())); }
    @Override @Transactional(readOnly = true)
    public boolean isBookingOwnedByUser(String ref, String username) { return getBookingForCustomer(ref, username).isPresent(); }
    @Override @Transactional(readOnly = true)
    public boolean isSlotAvailableForReschedule(LocalDate date, String slot, String ref) {
        return getBookingByReferenceNumber(ref).map(b -> scheduling.hasCapacity(date, slot, b.getEstimatedDuration(), ref)).orElse(false);
    }
    private void assertEditable(ServiceBooking b) {
        if ((!"Pending".equals(b.getStatus()) && !"Approved".equals(b.getStatus())) || jobRepository.findByReferenceNumber(b.getReferenceNumber()).isPresent()) throw new IllegalStateException("An allocated, completed or cancelled booking cannot be rescheduled; update its workshop allocation first");
        Invoice inv = invoiceRepository.findByReferenceNumber(b.getReferenceNumber()).orElse(null);
        if (inv != null && Rules.amount(inv.getRefundedAmount()) > 0) throw new IllegalStateException("A refunded service cannot be edited");
    }
    @Override
    public ServiceBooking rescheduleBooking(String ref, LocalDate date, String slot, String username) {
        ServiceBooking b = getBookingForCustomer(ref, username).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Booking is not yours"));
        return changeBooking(b, date, slot, b.getServiceType(), b.getNotes());
    }
    private ServiceBooking changeBooking(ServiceBooking b, LocalDate date, String slot, String service, String notes) {
        scheduling.lockResources(); assertEditable(b);
        b.setServiceDate(date); b.setTimeSlot(slot);
        if (!Objects.equals(b.getServiceType(), service)) { b.setServiceType(service); populatePackage(b); }
        scheduling.validateInterval(date, slot, b.getEstimatedDuration());
        if (!scheduling.hasCapacity(date, slot, b.getEstimatedDuration(), b.getReferenceNumber())) throw new IllegalStateException("No capacity remains for the service duration");
        validateNotes(notes); b.setNotes(notes); b.setProblemDescription(notes);
        ServiceBooking saved = bookingRepository.save(b); billing.updateServiceInvoice(saved); notificationService.bookingChanged(saved, "Booking updated"); return saved;
    }
    @Override
    public void cancelBookingForCustomer(String ref, String username) {
        getBookingForCustomer(ref, username).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Booking is not yours")); cancelBooking(ref);
    }
    @Override
    public ServiceBooking updateBookingStatus(String ref, String status) {
        ServiceBooking b = getBookingByReferenceNumber(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (Objects.equals(b.getStatus(), status)) return b;
        if ("Cancelled".equals(status)) { cancelBooking(ref); return b; }
        Optional<JobCard> job = jobRepository.findByReferenceNumber(ref);
        if (job.isPresent()) { workshop.updateJobCard(job.get().getId(), status, null, null); return bookingRepository.findByReferenceNumber(ref).orElseThrow(); }
        if (!List.of("Pending", "Approved").contains(status)) throw new IllegalStateException("Assign a mechanic and bay before starting or completing service");
        assertEditable(b); b.setStatus(status); b.setTrackingStage("Booking Confirmed"); bookingRepository.save(b); notificationService.bookingChanged(b, "Booking status updated"); return b;
    }
    @Override
    public ServiceBooking updateBookingAdmin(String ref, LocalDate date, String slot, String service) {
        ServiceBooking b = getBookingByReferenceNumber(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found")); return changeBooking(b, date, slot, service, b.getNotes());
    }
    @Override
    public ServiceBooking updateBookingAdmin(String ref, LocalDate date, String slot, String service, String notes) {
        ServiceBooking b = getBookingByReferenceNumber(ref).orElseThrow(() -> new IllegalArgumentException("Booking not found")); return changeBooking(b, date, slot, service, notes);
    }
    @Override @Transactional(readOnly = true)
    public List<ServiceBooking> searchBookingsForPOS(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim(); return getAllBookings().stream().filter(b -> !"Cancelled".equals(b.getStatus()) && (b.getReferenceNumber().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)) || b.getLicensePlate().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))).toList();
    }
    @Override @Transactional(readOnly = true)
    public String deriveCustomerType(ServiceBooking booking) {
        if (booking == null) return "GUEST_CUSTOMER";
        if (booking.getCustomerUsername() != null && !booking.getCustomerUsername().isBlank()) return "REGISTERED_CUSTOMER";
        if (booking.getLicensePlate() == null || booking.getLicensePlate().isBlank()) return "GUEST_CUSTOMER";
        try {
            return vehicleRepository.findFirstByLicensePlateIgnoreCase(booking.getLicensePlate().trim()).isPresent() ? "REGISTERED_VEHICLE" : "GUEST_CUSTOMER";
        } catch (Exception e) {
            return "GUEST_CUSTOMER";
        }
    }
}
