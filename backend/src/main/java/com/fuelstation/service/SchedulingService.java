package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
@Transactional(readOnly = true)
public class SchedulingService {
    private static List<String> generateAllStationSlots() {
        List<String> list = new ArrayList<>();
        LocalTime t = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(18, 0);
        while (t.isBefore(close)) {
            list.add(t.format(FORMAT));
            t = t.plusMinutes(15);
        }
        return Collections.unmodifiableList(list);
    }
    public static List<String> generateSlots(int intervalMinutes) {
        List<String> list = new ArrayList<>();
        LocalTime t = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(18, 0);
        while (t.isBefore(close)) {
            list.add(t.format(FORMAT));
            t = t.plusMinutes(intervalMinutes);
        }
        return Collections.unmodifiableList(list);
    }
    public static final List<String> SLOTS = generateAllStationSlots();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    @Autowired private ServiceBookingRepository bookings;
    @Autowired private JobCardRepository jobs;
    @Autowired private ServiceBayRepository bays;
    @Autowired private StaffRepository staff;
    @Autowired private AppUserRepository users;
    @Autowired private LeaveRequestRepository leaves;
    @Autowired private Clock clock;

    public static LocalTime time(String slot) {
        try { return LocalTime.parse(Rules.required(slot, "Time slot"), FORMAT); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Select a valid service time slot"); }
    }
    public static int durationMinutes(String value) {
        if (value == null || value.isBlank()) return 60;
        if (!value.trim().matches("(?i)(?:\\d+(?:\\.\\d+)?\\s*(?:hours?|hrs?|h|minutes?|mins?|m)\\s*)+")) throw new IllegalArgumentException("Enter a duration such as 1 Hour 30 Minutes");
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(hours?|hrs?|h|minutes?|mins?|m)", Pattern.CASE_INSENSITIVE).matcher(value);
        double minutes = 0;
        while (matcher.find()) minutes += Double.parseDouble(matcher.group(1)) * (matcher.group(2).toLowerCase(Locale.ROOT).startsWith("h") ? 60 : 1);
        if (minutes < 1 || minutes > 540) throw new IllegalArgumentException("Service duration must be between 1 minute and 9 hours");
        return (int) Math.ceil(minutes);
    }
    public static String formatTimeRange(String slot, String duration) {
        if (slot == null || slot.isBlank()) return "-";
        try {
            LocalTime start = time(slot);
            int minutes = durationMinutes(duration);
            LocalTime end = start.plusMinutes(minutes);
            return start.format(FORMAT) + " – " + end.format(FORMAT);
        } catch (Exception e) {
            return slot;
        }
    }
    public void validateInterval(LocalDate date, String slot, String duration) {
        if (date == null || date.isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("Select today or a future service date");
        if (!SLOTS.contains(slot)) throw new IllegalArgumentException("Select an available station time slot");
        LocalTime start = time(slot);
        if (date.equals(LocalDate.now(clock)) && !start.isAfter(LocalTime.now(clock))) throw new IllegalArgumentException("This time slot has already started");
        if (start.plusMinutes(durationMinutes(duration)).isAfter(LocalTime.of(18, 0))) throw new IllegalArgumentException("The service must finish within the station's 09:00–18:00 service hours");
    }
    @Transactional
    public void lockResources() { bays.lockForScheduling(); }
    public boolean isMechanic(Staff person) {
        if (person.getRole() == null) return false;
        String role = person.getRole().toLowerCase(Locale.ROOT);
        return role.contains("mechanic") || role.contains("tech") || role.contains("specialist");
    }
    public boolean activeMechanic(Staff person, LocalDate date) {
        if (!isMechanic(person) || "Inactive".equalsIgnoreCase(person.getEmploymentStatus()) || "Deactivated".equalsIgnoreCase(person.getEmploymentStatus())) return false;
        if (person.getSystemUsername() != null && !person.getSystemUsername().isBlank() && users.findByUsernameIgnoreCase(person.getSystemUsername()).filter(u -> "Active".equalsIgnoreCase(u.getStatus())).isEmpty()) return false;
        List<LeaveRequest> approved = leaves.findAll().stream().filter(l -> Objects.equals(l.getStaffId(), person.getId()) && "Approved".equalsIgnoreCase(l.getStatus())).toList();
        if (approved.stream().anyMatch(l -> l.getStartDate() != null && l.getEndDate() != null && !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))) return false;
        return !"On Leave".equalsIgnoreCase(person.getEmploymentStatus()) || !approved.isEmpty();
    }
    public boolean overlaps(ServiceBooking a, LocalDate date, String slot, String duration) {
        if (!date.equals(a.getServiceDate()) || Set.of("Cancelled","Completed").contains(a.getStatus())) return false;
        LocalTime start = time(slot), otherStart = time(a.getTimeSlot());
        return start.isBefore(otherStart.plusMinutes(durationMinutes(a.getEstimatedDuration()))) && otherStart.isBefore(start.plusMinutes(durationMinutes(duration)));
    }
    private boolean resourceFree(Long id, boolean mechanic, LocalDate date, String slot, String duration, String excludedRef) {
        for (JobCard job : jobs.findAll()) {
            if (Objects.equals(job.getReferenceNumber(), excludedRef) || Set.of("Cancelled","Completed").contains(job.getStatus())) continue;
            if (!Objects.equals(id, mechanic ? job.getMechanicId() : job.getBayId())) continue;
            ServiceBooking booking = bookings.findByReferenceNumber(job.getReferenceNumber()).orElse(null);
            if (booking != null && overlaps(booking, date, slot, duration)) return false;
        }
        return true;
    }
    public List<ServiceBay> availableBays(LocalDate date, String slot, String duration, String ref) {
        return bays.findAll().stream().filter(b -> !"Under Maintenance".equalsIgnoreCase(b.getStatus()) && !"Maintenance".equalsIgnoreCase(b.getStatus()) && !"Inactive".equalsIgnoreCase(b.getStatus()))
            .filter(b -> resourceFree(b.getId(), false, date, slot, duration, ref)).toList();
    }
    public List<Staff> availableMechanics(LocalDate date, String slot, String duration, String ref) {
        return staff.findAll().stream().filter(s -> activeMechanic(s, date)).filter(s -> resourceFree(s.getId(), true, date, slot, duration, ref)).toList();
    }
    public boolean hasCapacity(LocalDate date, String slot, String duration, String excludedRef) {
        try { validateInterval(date, slot, duration); } catch (IllegalArgumentException e) { return false; }
        long bayCount = bays.findAll().stream().filter(b -> !"Under Maintenance".equalsIgnoreCase(b.getStatus()) && !"Maintenance".equalsIgnoreCase(b.getStatus()) && !"Inactive".equalsIgnoreCase(b.getStatus())).count();
        long mechanicCount = staff.findAll().stream().filter(s -> activeMechanic(s, date)).count();
        long capacity = (bayCount > 0 && mechanicCount > 0) ? Math.min(bayCount, mechanicCount) : Math.max(bayCount, mechanicCount);
        if (capacity == 0 || (bayCount > 0 && availableBays(date, slot, duration, excludedRef).isEmpty()) || (mechanicCount > 0 && availableMechanics(date, slot, duration, excludedRef).isEmpty())) return false;
        List<ServiceBooking> overlapping = bookings.findAll().stream().filter(b -> !Objects.equals(b.getReferenceNumber(), excludedRef) && overlaps(b, date, slot, duration)).toList();
        LocalTime start = time(slot), end = start.plusMinutes(durationMinutes(duration));
        Set<LocalTime> points = new TreeSet<>(); points.add(start);
        for (ServiceBooking b : overlapping) { LocalTime t = time(b.getTimeSlot()); if (!t.isBefore(start) && t.isBefore(end)) points.add(t); }
        for (LocalTime point : points) {
            long count = overlapping.stream().filter(b -> !point.isBefore(time(b.getTimeSlot())) && point.isBefore(time(b.getTimeSlot()).plusMinutes(durationMinutes(b.getEstimatedDuration())))).count();
            if (count >= capacity) return false;
        }
        return true;
    }
    public void assertAllocation(ServiceBooking booking, Long mechanicId, Long bayId) {
        boolean freeBay = availableBays(booking.getServiceDate(), booking.getTimeSlot(), booking.getEstimatedDuration(), booking.getReferenceNumber()).stream().anyMatch(b -> Objects.equals(b.getId(), bayId));
        boolean freeMechanic = availableMechanics(booking.getServiceDate(), booking.getTimeSlot(), booking.getEstimatedDuration(), booking.getReferenceNumber()).stream().anyMatch(s -> Objects.equals(s.getId(), mechanicId));
        if (!freeBay || !freeMechanic) throw new IllegalStateException("The bay or mechanic is unavailable for the whole service duration");
    }
    public Map<String, Object> getAvailabilityInfo(LocalDate date, String duration, String excludedRef) {
        int durationMins = durationMinutes(duration);
        int step = (durationMins % 30 != 0) ? 15 : 30;
        List<String> slotList = generateSlots(step);
        
        long bayCount = bays.findAll().stream().filter(b -> !"Under Maintenance".equalsIgnoreCase(b.getStatus()) && !"Maintenance".equalsIgnoreCase(b.getStatus()) && !"Inactive".equalsIgnoreCase(b.getStatus())).count();
        long mechanicCount = staff.findAll().stream().filter(s -> activeMechanic(s, date)).count();
        long capacity = (bayCount > 0 && mechanicCount > 0) ? Math.min(bayCount, mechanicCount) : Math.max(bayCount, mechanicCount);

        List<ServiceBooking> activeBookings = bookings.findAll().stream()
            .filter(b -> date.equals(b.getServiceDate()) && !Set.of("Cancelled","Completed").contains(b.getStatus()) && !Objects.equals(b.getReferenceNumber(), excludedRef))
            .toList();

        List<Map<String, Object>> slotsResult = new ArrayList<>();
        for (String slot : slotList) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeSlot", slot);
            item.put("timeRange", formatTimeRange(slot, duration));
            LocalTime slotTime = time(slot);
            LocalTime slotEnd = slotTime.plusMinutes(step);

            List<ServiceBooking> slotBookings = activeBookings.stream().filter(b -> {
                LocalTime bStart = time(b.getTimeSlot());
                LocalTime bEnd = bStart.plusMinutes(durationMinutes(b.getEstimatedDuration()));
                return slotTime.isBefore(bEnd) && bStart.isBefore(slotEnd);
            }).toList();

            boolean hasCap = hasCapacity(date, slot, duration, excludedRef);
            item.put("available", hasCap);
            item.put("slotOccupied", !slotBookings.isEmpty());
            item.put("activeBookingsCount", slotBookings.size());
            item.put("capacity", capacity);
            item.put("isFull", capacity > 0 && slotBookings.size() >= capacity);
            slotsResult.add(item);
        }

        List<Map<String, Object>> existingList = activeBookings.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("referenceNumber", b.getReferenceNumber());
            m.put("timeSlot", b.getTimeSlot());
            m.put("timeRange", formatTimeRange(b.getTimeSlot(), b.getEstimatedDuration()));
            m.put("serviceType", b.getServiceType());
            m.put("customerName", b.getCustomerName());
            return m;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("slots", slotsResult);
        result.put("capacity", capacity);
        result.put("slotStep", step);
        result.put("durationMinutes", durationMins);
        result.put("existingBookings", existingList);
        return result;
    }
}
