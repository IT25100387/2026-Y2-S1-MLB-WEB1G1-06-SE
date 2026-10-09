package com.fuelstation.service;

import com.fuelstation.model.CustomerFeedback;
import com.fuelstation.model.JobCard;
import com.fuelstation.model.ServiceBay;
import com.fuelstation.model.Staff;

import java.util.List;
import java.util.Optional;

public interface WorkshopService {
    List<JobCard> getAllJobCards();
    Optional<JobCard> getJobCardById(Long id);
    Optional<JobCard> getJobCardByReferenceNumber(String referenceNumber);
    JobCard createJobCard(String referenceNumber, Long mechanicId, Long bayId);
    JobCard updateJobCard(Long id, String status, String mechanicNotes, String partsUsed);
    java.util.List<com.fuelstation.model.JobPartUsage> getUsedParts(Long jobId);
    com.fuelstation.model.JobPartUsage addUsedPart(Long jobId, Long partId, Integer quantity, String requestKey);
    
    List<ServiceBay> getAllBays();
    ServiceBay addBay(ServiceBay bay);
    void deleteBay(Long id);
    
    List<Staff> getAvailableMechanics();
    
    List<CustomerFeedback> getAllFeedbacks();
    CustomerFeedback submitFeedback(CustomerFeedback feedback);
    List<com.fuelstation.dto.ScheduleSlotDTO> getMechanicScheduleForDate(Long mechanicId, java.time.LocalDate date);
    List<com.fuelstation.dto.ScheduleSlotDTO> getBayScheduleForDate(Long bayId, java.time.LocalDate date);
    List<String> getMechanicAssignedDates(Long mechanicId);
    List<String> getBayAssignedDates(Long bayId);
}
