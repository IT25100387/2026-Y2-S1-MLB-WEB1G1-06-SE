package com.fuelstation.service;

import com.fuelstation.model.SupportTicket;
import java.util.List;
import java.util.Optional;

public interface SupportTicketService {
    SupportTicket createTicket(SupportTicket ticket, String username);
    List<SupportTicket> getTicketsForCustomer(String username);
    List<SupportTicket> getAllTickets();
    Optional<SupportTicket> getTicketById(Long id);
    void resolveTicket(Long id, String reply, String status);
    long getOpenTicketsCount();
}
