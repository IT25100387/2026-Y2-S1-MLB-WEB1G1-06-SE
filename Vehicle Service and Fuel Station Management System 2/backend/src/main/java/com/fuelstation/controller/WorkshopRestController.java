package com.fuelstation.controller;

import com.fuelstation.service.BookingService;
import com.fuelstation.service.WorkshopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workshop")
public class WorkshopRestController {

    @Autowired
    private WorkshopService workshopService;

    @Autowired
    private BookingService bookingService;
    @Autowired private com.fuelstation.service.StaffService hr;
    @Autowired private com.fuelstation.service.SchedulingService scheduling;
    @Autowired private com.fuelstation.service.BillingService billing;
    @GetMapping("/jobs/workspace") public Map<String,Object> workspace() {
        var rows=workshopService.getAllJobCards().stream().map(job -> { Map<String,Object> row=new HashMap<>(); row.put("job",job); row.put("booking",bookingService.getBookingByReferenceNumber(job.getReferenceNumber()).orElse(null)); row.put("usedParts",workshopService.getUsedParts(job.getId())); return row; }).toList();
        return Map.of("jobs",rows,"parts",billing.getAllSpareParts().stream().map(p -> Map.of("id",p.getId(),"partName",p.getPartName(),"stockQuantity",p.getStockQuantity())).toList());
    }

    @GetMapping("/availability/{referenceNumber}")
    public Map<String, Object> availability(@org.springframework.web.bind.annotation.PathVariable String referenceNumber) {
        var b = bookingService.getBookingByReferenceNumber(referenceNumber).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        return Map.of("bays", scheduling.availableBays(b.getServiceDate(), b.getTimeSlot(), b.getEstimatedDuration(), referenceNumber),
            "mechanics", scheduling.availableMechanics(b.getServiceDate(), b.getTimeSlot(), b.getEstimatedDuration(), referenceNumber));
    }
    @GetMapping("/jobs/{id}/parts")
    public Map<String, Object> usedParts(@org.springframework.web.bind.annotation.PathVariable Long id) { return Map.of("parts", workshopService.getUsedParts(id)); }
    public record PartUsageRequest(Long partId, Integer quantity) {}
    @org.springframework.web.bind.annotation.PostMapping("/jobs/{id}/parts")
    public Map<String, Object> addPart(@org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody PartUsageRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return Map.of("success", true, "part", workshopService.addUsedPart(id, request.partId(), request.quantity(), key));
    }

    @GetMapping("/mechanics/schedules")
    public Map<String, Object> getMechanicSchedules() {
        Map<String, Object> response = new HashMap<>();

        // 1. Get all available mechanics
        List<com.fuelstation.model.Staff> mechanics = workshopService.getAvailableMechanics();
        List<Map<String, Object>> mechanicsList = new ArrayList<>();
        
        for (com.fuelstation.model.Staff mech : mechanics) {
            Map<String, Object> mechMap = new HashMap<>();
            mechMap.put("id", mech.getId());
            mechMap.put("name", mech.getName());
            mechMap.put("status", mech.getEmploymentStatus());
            mechanicsList.add(mechMap);
        }
        
        // 2. Get active jobs mapped to mechanics
        List<com.fuelstation.model.JobCard> activeJobs = workshopService.getAllJobCards().stream()
                .filter(j -> !"Completed".equalsIgnoreCase(j.getStatus()))
                .toList();
        
        Map<Long, List<Map<String, Object>>> mechanicSchedules = new HashMap<>();
        
        for (com.fuelstation.model.JobCard job : activeJobs) {
            if (job.getMechanicId() != null) {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("vehicle", job.getLicensePlate() != null ? job.getLicensePlate() : "-");
                scheduleMap.put("status", job.getStatus());
                scheduleMap.put("serviceType", job.getServiceType() != null ? job.getServiceType() : "-");
                scheduleMap.put("bayName", job.getBayName() != null ? job.getBayName() : "-");
                
                if (job.getReferenceNumber() != null) {
                    com.fuelstation.model.ServiceBooking b = null;
                    if (job.getReferenceNumber().startsWith("ID-")) {
                        b = bookingService.getBookingById(Long.parseLong(job.getReferenceNumber().replace("ID-", ""))).orElse(null);
                    } else if (job.getReferenceNumber().matches("\\d+")) {
                        b = bookingService.getBookingById(Long.parseLong(job.getReferenceNumber())).orElse(null);
                    } else {
                        b = bookingService.getBookingByReferenceNumber(job.getReferenceNumber()).orElse(null);
                    }
                    if (b != null) {
                        scheduleMap.put("timeSlot", b.getTimeSlot() != null ? b.getTimeSlot() : "-");
                        scheduleMap.put("date", b.getServiceDate() != null ? b.getServiceDate().toString() : "-");
                    } else {
                        scheduleMap.put("timeSlot", "-");
                        scheduleMap.put("date", "-");
                    }
                } else {
                    scheduleMap.put("timeSlot", "-");
                    scheduleMap.put("date", "-");
                }
                
                mechanicSchedules.computeIfAbsent(job.getMechanicId(), k -> new ArrayList<>()).add(scheduleMap);
            }
        }
        
        response.put("mechanics", mechanicsList);
        response.put("schedules", mechanicSchedules);
        
        return response;
    }

    @GetMapping
    public Map<String, Object> getWorkshopData() {
        Map<String, Object> response = new HashMap<>();
        response.put("jobCards", workshopService.getAllJobCards());
        response.put("bays", workshopService.getAllBays());
        response.put("mechanics", hr.getAllStaff().stream().filter(scheduling::isMechanic).toList());
        response.put("bookings", bookingService.getPendingBookings());
        return response;
    }

    @GetMapping("/bays")
    public Map<String, Object> getBaySchedules() {
        Map<String, Object> response = new HashMap<>();
        response.put("bays", workshopService.getAllBays());
        
        List<com.fuelstation.model.JobCard> activeJobs = workshopService.getAllJobCards().stream()
                .filter(j -> !"Completed".equalsIgnoreCase(j.getStatus()))
                .toList();
        
        Map<Long, List<Map<String, Object>>> baySchedules = new HashMap<>();
        
        for (com.fuelstation.model.JobCard job : activeJobs) {
            if (job.getBayId() != null) {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("licensePlate", job.getLicensePlate());
                scheduleMap.put("status", job.getStatus());
                scheduleMap.put("serviceType", job.getServiceType() != null ? job.getServiceType() : "-");
                scheduleMap.put("mechanicName", job.getMechanicName() != null ? job.getMechanicName() : "-");
                
                if (job.getReferenceNumber() != null) {
                    com.fuelstation.model.ServiceBooking b = null;
                    if (job.getReferenceNumber().startsWith("ID-")) {
                        b = bookingService.getBookingById(Long.parseLong(job.getReferenceNumber().replace("ID-", ""))).orElse(null);
                    } else if (job.getReferenceNumber().matches("\\d+")) {
                        b = bookingService.getBookingById(Long.parseLong(job.getReferenceNumber())).orElse(null);
                    } else {
                        b = bookingService.getBookingByReferenceNumber(job.getReferenceNumber()).orElse(null);
                    }
                    if (b != null) {
                        scheduleMap.put("timeSlot", b.getTimeSlot());
                        scheduleMap.put("serviceDate", b.getServiceDate());
                    }
                }
                
                baySchedules.computeIfAbsent(job.getBayId(), k -> new ArrayList<>()).add(scheduleMap);
            }
        }
        
        response.put("baySchedules", baySchedules);
        return response;
    }

    @org.springframework.web.bind.annotation.PostMapping("/bay/add")
    public Map<String, Object> addBay(@org.springframework.web.bind.annotation.RequestBody com.fuelstation.model.ServiceBay bay) {
        bay.setId(null);
        workshopService.addBay(bay);
        return Map.of("success", true);
    }

    @org.springframework.web.bind.annotation.PostMapping("/bay/update/{id}")
    public Object updateBay(@org.springframework.web.bind.annotation.PathVariable Long id,@org.springframework.web.bind.annotation.RequestBody com.fuelstation.model.ServiceBay bay){bay.setId(id);return workshopService.addBay(bay);}
    @org.springframework.web.bind.annotation.DeleteMapping("/bay/{id}")
    public Object deleteBay(@org.springframework.web.bind.annotation.PathVariable Long id){workshopService.deleteBay(id);return Map.of("success",true);}

    @org.springframework.web.bind.annotation.PostMapping("/job/create")
    public Map<String, Object> createJobCard(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        String referenceNumber = (String) request.get("referenceNumber");
        Long mechanicId = request.get("mechanicId") != null ? Long.valueOf(request.get("mechanicId").toString()) : null;
        Long bayId = request.get("bayId") != null ? Long.valueOf(request.get("bayId").toString()) : null;
        workshopService.createJobCard(referenceNumber, mechanicId, bayId);
        return Map.of("success", true);
    }

    @org.springframework.web.bind.annotation.PostMapping("/job/update/{id}")
    public Map<String, Object> updateJobCard(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> request
    ) {
        String status = request.get("status");
        String mechanicNotes = request.get("mechanicNotes");
        String partsUsed = request.get("partsUsed");
        workshopService.updateJobCard(id, status, mechanicNotes, partsUsed);
        return Map.of("success", true);
    }

    @GetMapping("/availability")
    public java.util.Map<String, Object> getAvailabilityApi(@org.springframework.web.bind.annotation.RequestParam String date, @org.springframework.web.bind.annotation.RequestParam String timeSlot) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        java.util.List<java.util.Map<String, Object>> availableBays = new java.util.ArrayList<>();
        for (com.fuelstation.model.ServiceBay b : workshopService.getAllBays()) {
            boolean assigned = workshopService.getBayScheduleForDate(b.getId(), localDate)
                .stream().anyMatch(s -> s.isAssigned() && s.getTimeSlot().equals(timeSlot));
            if (!assigned && "Available".equals(b.getStatus())) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", b.getId());
                map.put("name", b.getBayName());
                availableBays.add(map);
            }
        }
        
        java.util.List<java.util.Map<String, Object>> availableMechanics = new java.util.ArrayList<>();
        for (com.fuelstation.model.Staff m : workshopService.getAvailableMechanics()) {
            boolean assigned = workshopService.getMechanicScheduleForDate(m.getId(), localDate)
                .stream().anyMatch(s -> s.isAssigned() && s.getTimeSlot().equals(timeSlot));
            if (!assigned && "Active".equals(m.getEmploymentStatus())) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("name", m.getName() + " (" + (m.getRole() != null ? m.getRole() : "Technician") + ")");
                availableMechanics.add(map);
            }
        }
        
        response.put("bays", availableBays);
        response.put("mechanics", availableMechanics);
        return response;
    }
}
