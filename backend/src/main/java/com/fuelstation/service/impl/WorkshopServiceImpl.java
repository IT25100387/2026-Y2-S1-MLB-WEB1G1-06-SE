package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class WorkshopServiceImpl implements WorkshopService {
    @Autowired private JobCardRepository jobCardRepository;
    @Autowired private ServiceBookingRepository bookingRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private ServiceBayRepository bayRepository;
    @Autowired private CustomerFeedbackRepository feedbackRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private SparePartRepository partRepository;
    @Autowired private JobPartUsageRepository usageRepository;
    @Autowired private BillingService billing;
    @Autowired private OfferService offers;
    @Autowired private SchedulingService scheduling;
    @Autowired private Clock clock;

    private boolean isMechanicUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_MECHANIC".equals(a.getAuthority()));
    }
    private void requireOwnership(JobCard job) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (isMechanicUser() && !Objects.equals(auth.getName(), job.getMechanicUsername())) throw new AccessDeniedException("This job is not allocated to you");
    }
    @Override @Transactional(readOnly = true)
    public List<JobCard> getAllJobCards() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return isMechanicUser() ? jobCardRepository.findByMechanicUsername(auth.getName()) : jobCardRepository.findAll();
    }
    @Override @Transactional(readOnly = true)
    public Optional<JobCard> getJobCardById(Long id) { Optional<JobCard> job = jobCardRepository.findById(id); job.ifPresent(this::requireOwnership); return job; }
    @Override @Transactional(readOnly = true)
    public Optional<JobCard> getJobCardByReferenceNumber(String ref) { Optional<JobCard> job = jobCardRepository.findByReferenceNumber(ref); job.ifPresent(this::requireOwnership); return job; }
    @Override
    public JobCard createJobCard(String ref, Long mechanicId, Long bayId) {
        scheduling.lockResources();
        ServiceBooking booking = bookingRepository.findByReferenceNumber(Rules.required(ref, "Service reference")).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!List.of("Pending", "Approved").contains(booking.getStatus())) throw new IllegalStateException("Only an unallocated pending/approved service can receive a job card");
        if (jobCardRepository.findByReferenceNumber(ref).isPresent()) throw new IllegalStateException("This service already has a job card");
        scheduling.assertAllocation(booking, mechanicId, bayId);
        Staff mechanic = staffRepository.findById(mechanicId).orElseThrow(() -> new IllegalArgumentException("Mechanic not found"));
        ServiceBay bay = bayRepository.findById(bayId).orElseThrow(() -> new IllegalArgumentException("Bay not found"));
        JobCard job = new JobCard(); job.setReferenceNumber(ref); job.setLicensePlate(booking.getLicensePlate()); job.setCustomerName(booking.getCustomerName()); job.setServiceType(booking.getServiceType());
        job.setMechanicId(mechanic.getId()); job.setMechanicName(mechanic.getName()); job.setMechanicUsername(mechanic.getSystemUsername()); job.setBayId(bay.getId()); job.setBayName(bay.getBayName()); job.setStatus("Approved");
        jobCardRepository.save(job);
        booking.setStatus("Approved"); booking.setTrackingStage("Mechanic & Bay Assigned"); bookingRepository.save(booking);
        notificationService.jobChanged(job, "Service job allocated");
        notificationService.createCustomerNotification(booking.getCustomerUsername(), "Mechanic allocated", ref + " is assigned to " + mechanic.getName(), "Service", "/customer/track/" + ref, "fa-wrench");
        return job;
    }
    private boolean transition(String current, String next) {
        if (Objects.equals(current, next)) return true;
        return switch (current) {
            case "Pending", "Approved", "Assigned" -> "In Progress".equals(next);
            case "In Progress" -> List.of("Waiting for Parts", "Quality Check", "Completed").contains(next);
            case "Waiting for Parts" -> List.of("In Progress", "Completed").contains(next);
            case "Quality Check" -> List.of("In Progress", "Completed").contains(next);
            default -> false;
        };
    }
    @Override
    public JobCard updateJobCard(Long id, String status, String notes, String partsUsed) {
        JobCard job = jobCardRepository.lockById(id).orElseThrow(() -> new IllegalArgumentException("Job not found")); requireOwnership(job);
        if ("Completed".equals(job.getStatus()) || "Cancelled".equals(job.getStatus())) throw new IllegalStateException("A completed/cancelled job cannot be changed");
        String next = status == null ? job.getStatus() : status;
        if (!transition(job.getStatus(), next)) throw new IllegalStateException("Invalid job transition from " + job.getStatus() + " to " + next);
        ServiceBooking booking = bookingRepository.findByReferenceNumber(job.getReferenceNumber()).orElseThrow(() -> new IllegalStateException("Job service reference is missing"));
        if ("Cancelled".equals(booking.getStatus())) throw new IllegalStateException("A cancelled service cannot continue");
        if (List.of("Pending", "Approved", "Assigned").contains(job.getStatus()) && "In Progress".equals(next)) {
            LocalDateTime end=JobExpiryService.scheduledEnd(booking);
            if (end != null && LocalDateTime.now(clock).isAfter(end)) throw new IllegalStateException("This job's scheduled end has passed; it cannot be started");
        }
        if (!Objects.equals(next, job.getStatus()) && booking.getServiceDate().isAfter(LocalDate.now(clock))) throw new IllegalStateException("This job is scheduled for a future service date");
        if (partsUsed != null && !partsUsed.isBlank() && !Objects.equals(partsUsed, job.getPartsUsed())) throw new IllegalArgumentException("Record spare parts using the part selector and quantity; free text does not change stock");
        if (notes != null) { if (notes.length() > 4000) throw new IllegalArgumentException("Notes are too long"); job.setMechanicNotes(notes); }
        boolean changed = !Objects.equals(job.getStatus(), next);
        job.setStatus(next); jobCardRepository.save(job);
        switch (next) {
            case "Approved", "Assigned", "Pending" -> { booking.setStatus("Approved"); booking.setTrackingStage("Mechanic & Bay Assigned"); }
            case "Quality Check" -> { booking.setStatus("In Progress"); booking.setTrackingStage("Quality Check"); }
            case "Completed" -> { booking.setStatus("Completed"); booking.setTrackingStage("Completed & Ready"); }
            default -> { booking.setStatus("In Progress"); booking.setTrackingStage(next); }
        }
        bookingRepository.save(booking);
        if ("Completed".equals(next)) billing.updateServiceInvoice(booking);
        bayRepository.findById(job.getBayId()).ifPresent(b -> {
            boolean active = jobCardRepository.findByBayId(b.getId()).stream().anyMatch(j -> List.of("In Progress", "Waiting for Parts", "Quality Check").contains(j.getStatus()));
            if (!"Under Maintenance".equals(b.getStatus())) { b.setStatus(active ? "Occupied" : "Available"); bayRepository.save(b); }
        });
        if (changed) {
            notificationService.jobChanged(job, "Completed".equals(next) ? "Service completed" : "Job progress updated");
            notificationService.createCustomerNotification(booking.getCustomerUsername(), "Service progress updated", booking.getReferenceNumber() + " | " + booking.getTrackingStage(), "Service", "/customer/track/" + booking.getReferenceNumber(), "fa-wrench");
        }
        return job;
    }
    @Override @Transactional(readOnly = true)
    public List<JobPartUsage> getUsedParts(Long id) { getJobCardById(id).orElseThrow(() -> new IllegalArgumentException("Job not found")); return usageRepository.findByJobCardIdOrderByIdAsc(id); }
    @Override
    public JobPartUsage addUsedPart(Long jobId, Long partId, Integer quantity, String key) {
        JobCard job = jobCardRepository.lockById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found")); requireOwnership(job);
        int qty = Rules.quantity(quantity, "Used quantity");
        if (qty > 9999) throw new IllegalArgumentException("Used quantity must not exceed 9999");
        if (key != null && !key.isBlank()) {
            Optional<JobPartUsage> prior = usageRepository.findByRequestKey(key);
            if (prior.isPresent()) {
                JobPartUsage u = prior.get();
                if (!Objects.equals(u.getJobCardId(), jobId) || !Objects.equals(u.getPartId(), partId) || !Objects.equals(u.getQuantity(), qty)) throw new IllegalArgumentException("Part request key already used");
                return u;
            }
        }
        if (!List.of("In Progress", "Waiting for Parts").contains(job.getStatus())) throw new IllegalStateException("Start the assigned job before recording used parts");
        SparePart part = partRepository.lockById(partId).orElseThrow(() -> new IllegalArgumentException("Part not found"));
        if (part.getStockQuantity() == null || part.getStockQuantity() < qty) throw new IllegalArgumentException("Not enough spare part stock");
        if("Inactive".equalsIgnoreCase(part.getStatus()) || "Deactivated".equalsIgnoreCase(part.getStatus())) throw new IllegalStateException("This spare part is inactive");
        double unitPrice = Rules.nonnegative(part.getSellingPrice(), "Selling price");
        for(Offer offer:offers.getActiveOffersByType(OfferTargetType.SPARE_PART)) if(partId.equals(offer.getTargetId())){unitPrice=Rules.money(unitPrice*(1-offer.getDiscountPercentage()/100));break;}
        JobPartUsage usage = new JobPartUsage(); usage.setJobCardId(jobId); usage.setReferenceNumber(job.getReferenceNumber()); usage.setPartId(partId); usage.setPartName(part.getPartName()); usage.setQuantity(qty); usage.setUnitPrice(unitPrice); usage.setTotalAmount(Rules.money(unitPrice * qty)); usage.setRecordedAt(LocalDateTime.now(clock)); usage.setRequestKey(key);
        var auth = SecurityContextHolder.getContext().getAuthentication(); usage.setRecordedBy(auth == null ? "system" : auth.getName());
        part.setStockQuantity(part.getStockQuantity() - qty); part.setStatus(part.getStockQuantity() <= (part.getMinStockWarning() == null ? 0 : part.getMinStockWarning()) ? "Low Stock" : "Available"); partRepository.save(part); usageRepository.save(usage);
        job.setPartsUsed(usageRepository.findByJobCardIdOrderByIdAsc(jobId).stream().map(u -> u.getPartName() + " x" + u.getQuantity()).collect(java.util.stream.Collectors.joining(", "))); jobCardRepository.save(job);
        ServiceBooking b = bookingRepository.findByReferenceNumber(job.getReferenceNumber()).orElseThrow(); billing.updateServiceInvoice(b);
        notificationService.jobChanged(job, "Used spare part recorded"); notificationService.createCustomerNotification(b.getCustomerUsername(), "Service parts updated", part.getPartName() + " x" + qty + " recorded for " + b.getReferenceNumber(), "Service", "/customer/track/" + b.getReferenceNumber(), "fa-box");
        if ("Low Stock".equals(part.getStatus())) notificationService.notifyRoles("Spare part stock low", part.getPartName() + " has " + part.getStockQuantity() + " units remaining", "Inventory", "/dashboard/inventory/parts", "MANAGER");
        if ("Low Stock".equals(part.getStatus())) notificationService.notifyRoles("Spare part stock low",part.getPartName(),"Inventory","/dashboard/pos","CASHIER");
        return usage;
    }
    @Override @Transactional(readOnly = true)
    public List<ServiceBay> getAllBays() { return bayRepository.findAll(); }
    @Override
    public ServiceBay addBay(ServiceBay bay) {
        bay.setBayName(Rules.required(bay.getBayName(), "Bay name"));
        if (bay.getStatus()==null) bay.setStatus("Available");
        if (!Set.of("Available","Occupied","Under Maintenance").contains(bay.getStatus())) throw new IllegalArgumentException("Invalid bay state");
        if (bayRepository.findAll().stream().anyMatch(b -> b.getBayName().equalsIgnoreCase(bay.getBayName()) && !Objects.equals(b.getId(),bay.getId()))) throw new IllegalArgumentException("Bay name already exists");
        if (bay.getId()!=null) {
            ServiceBay old=bayRepository.findById(bay.getId()).orElseThrow(() -> new IllegalArgumentException("Bay not found"));
            var linked=jobCardRepository.findAll().stream().filter(j -> bay.getId().equals(j.getBayId())).toList();
            if (!old.getBayName().equals(bay.getBayName())&&!linked.isEmpty()) throw new IllegalStateException("Keep the bay name to preserve linked job history");
            if ("Under Maintenance".equals(bay.getStatus())&&linked.stream().anyMatch(j -> !Set.of("Completed","Cancelled").contains(j.getStatus()))) throw new IllegalStateException("Finish or reassign the bay's jobs before maintenance");
        }
        return bayRepository.save(bay);
    }
    @Override public void deleteBay(Long id) {
        bayRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bay not found"));
        if (jobCardRepository.findAll().stream().anyMatch(j -> id.equals(j.getBayId()))) throw new IllegalStateException("This bay has job history. Retain it and use maintenance state.");
        bayRepository.deleteById(id);
    }
    @Override @Transactional(readOnly = true)
    public List<Staff> getAvailableMechanics() { return staffRepository.findAll().stream().filter(s -> scheduling.activeMechanic(s, LocalDate.now(clock))).toList(); }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderBySubmissionDateDesc();
    }

    @Override
    @Transactional
    public CustomerFeedback submitFeedback(CustomerFeedback feedback) {
        if (feedback.getRating() == null || feedback.getRating() < 1 || feedback.getRating() > 5) throw new IllegalArgumentException("Choose a rating from 1 to 5");
        feedback.setId(null); feedback.setCustomerName(com.fuelstation.util.Rules.required(feedback.getCustomerName(), "Customer name"));
        if (feedback.getComments() != null && feedback.getComments().length() > 2000) throw new IllegalArgumentException("Feedback is too long");
        if (feedback.getJobCardId() != null) {
            JobCard job = jobCardRepository.findById(feedback.getJobCardId()).orElseThrow(() -> new IllegalArgumentException("Job not found"));
            ServiceBooking b = bookingRepository.findByReferenceNumber(job.getReferenceNumber()).orElseThrow();
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority())) && !auth.getName().equals(b.getCustomerUsername())) throw new org.springframework.security.access.AccessDeniedException("This service is not yours");
            if (!"Completed".equals(job.getStatus())) throw new IllegalArgumentException("Submit service feedback after the job is completed");
            if (feedbackRepository.findAll().stream().anyMatch(f -> feedback.getJobCardId().equals(f.getJobCardId()))) throw new IllegalArgumentException("Feedback is already recorded for this job");
            feedback.setServiceType(b.getServiceType());
        }
        feedback.setSubmissionDate(LocalDateTime.now(clock));
        feedback.setNeedsAttention(feedback.getRating() != null && feedback.getRating() < 3);
        CustomerFeedback saved = feedbackRepository.save(feedback);

        if (Boolean.TRUE.equals(saved.getNeedsAttention())) {
            notificationService.createNotification(
                    "Low Rating Alert",
                    "Customer " + saved.getCustomerName() + " submitted a rating of " + saved.getRating() + ".",
                    "Alert"
            );
        }

        return saved;
    }

    private List<com.fuelstation.dto.ScheduleSlotDTO> getScheduleForDate(List<JobCard> jobs, java.time.LocalDate date) {
        List<String> timeSlots = java.util.Arrays.asList("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM");
        List<com.fuelstation.dto.ScheduleSlotDTO> schedule = new java.util.ArrayList<>();
        for (String slot : timeSlots) {
            schedule.add(new com.fuelstation.dto.ScheduleSlotDTO(slot));
        }
        
        List<String> refNumbers = jobs.stream().map(JobCard::getReferenceNumber).filter(r -> r != null).toList();
        if (refNumbers.isEmpty()) return schedule;
        
        List<ServiceBooking> bookings = bookingRepository.findByReferenceNumberIn(refNumbers);
        
        for (ServiceBooking booking : bookings) {
            if (date.equals(booking.getServiceDate()) && !"Cancelled".equals(booking.getStatus())) {
                schedule.stream().filter(s -> scheduling.overlaps(booking, date, s.getTimeSlot(), "1 minute")).forEach(slot -> {
                    slot.setAssigned(true);
                    slot.setLicensePlate(booking.getLicensePlate());
                    slot.setServiceType(booking.getServiceType());
                    
                    jobs.stream().filter(j -> j.getReferenceNumber() != null && booking.getReferenceNumber().equals(j.getReferenceNumber())).findFirst().ifPresent(job -> {
                        slot.setAllocatedBay(job.getBayName());
                        slot.setMechanicName(job.getMechanicName());
                    });
                });
            }
        }
        return schedule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.fuelstation.dto.ScheduleSlotDTO> getMechanicScheduleForDate(Long mechanicId, java.time.LocalDate date) {
        List<JobCard> jobs = jobCardRepository.findByMechanicId(mechanicId);
        return getScheduleForDate(jobs, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.fuelstation.dto.ScheduleSlotDTO> getBayScheduleForDate(Long bayId, java.time.LocalDate date) {
        List<JobCard> jobs = jobCardRepository.findByBayId(bayId);
        return getScheduleForDate(jobs, date);
    }

    private List<String> getAssignedDatesForJobs(List<JobCard> jobs) {
        List<String> refNumbers = jobs.stream().map(JobCard::getReferenceNumber).filter(r -> r != null).toList();
        if (refNumbers.isEmpty()) return new java.util.ArrayList<>();
        return bookingRepository.findByReferenceNumberIn(refNumbers).stream()
                .filter(b -> !"Cancelled".equals(b.getStatus()))
                .map(b -> b.getServiceDate().toString())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMechanicAssignedDates(Long mechanicId) {
        return getAssignedDatesForJobs(jobCardRepository.findByMechanicId(mechanicId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBayAssignedDates(Long bayId) {
        return getAssignedDatesForJobs(jobCardRepository.findByBayId(bayId));
    }
}
