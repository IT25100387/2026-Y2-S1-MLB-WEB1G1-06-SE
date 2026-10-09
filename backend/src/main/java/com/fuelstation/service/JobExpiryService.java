package com.fuelstation.service;

import com.fuelstation.model.ServiceBooking;
import com.fuelstation.repository.JobCardRepository;
import com.fuelstation.repository.ServiceBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.List;

@Service
public class JobExpiryService {
    private static final List<String> UNSTARTED = List.of("Approved", "Assigned", "Pending");
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobExpiryService.class);
    private final JobCardRepository jobs;
    private final ServiceBookingRepository bookings;
    private final BillingService billing;
    private final NotificationService notifications;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public JobExpiryService(JobCardRepository jobs, ServiceBookingRepository bookings, BillingService billing,
                            NotificationService notifications, Clock clock, PlatformTransactionManager transactions) {
        this.jobs=jobs; this.bookings=bookings; this.billing=billing; this.notifications=notifications;
        this.clock=clock; this.transaction=new TransactionTemplate(transactions);
    }
    public static LocalDateTime scheduledEnd(ServiceBooking booking) {
        if (booking == null || booking.getServiceDate() == null || booking.getTimeSlot() == null) return null;
        return booking.getServiceDate().atTime(SchedulingService.time(booking.getTimeSlot()))
            .plusMinutes(SchedulingService.durationMinutes(booking.getEstimatedDuration()));
    }
    public boolean expireJob(Long id) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            var job=jobs.lockById(id).orElse(null);
            if (job == null || !UNSTARTED.contains(job.getStatus())) return false;
            var booking=bookings.findByReferenceNumber(job.getReferenceNumber()).orElse(null);
            LocalDateTime end=scheduledEnd(booking);
            if (end == null || !LocalDateTime.now(clock).isAfter(end)) return false;
            job.setStatus("Cancelled"); jobs.save(job);
            booking.setStatus("Cancelled"); booking.setTrackingStage("Cancelled - scheduled end passed without starting"); bookings.save(booking);
            billing.closeExpiredServiceInvoice(booking);
            notifications.jobChanged(job,"Unstarted service automatically cancelled");
            notifications.bookingChanged(booking,"Unstarted service automatically cancelled");
            return true;
        }));
    }
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public int expireUnstartedJobs() {
        int count=0;
        for (var job:jobs.findByStatusIn(UNSTARTED)) {
            try { if (expireJob(job.getId())) count++; }
            catch (RuntimeException failure) { log.warn("Could not expire job {}: {}",job.getReferenceNumber(),failure.getMessage()); }
        }
        return count;
    }
}
