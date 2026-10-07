package com.fuelstation.service.impl;

import com.fuelstation.model.SupportTicket;
import com.fuelstation.repository.SupportTicketRepository;
import com.fuelstation.service.NotificationService;
import com.fuelstation.service.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupportTicketServiceImpl implements SupportTicketService {

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private NotificationService notificationService;
    @Autowired private java.time.Clock clock;

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket ticket, String username) {
        ticket.setId(null);
        ticket.setSubject(com.fuelstation.util.Rules.required(ticket.getSubject(), "Subject"));
        ticket.setMessage(com.fuelstation.util.Rules.required(ticket.getMessage(), "Message"));
        if (ticket.getSubject().length() > 150 || ticket.getMessage().length() > 4000) throw new IllegalArgumentException("Subject or message is too long");
        ticket.setAdminReply(null); ticket.setResolvedDate(null);
        ticket.setCustomerUsername(username);
        ticket.setTicketNumber("TCK-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        ticket.setStatus("OPEN");
        ticket.setCreatedDate(LocalDateTime.now(clock));
        if (ticket.getPriority() == null || ticket.getPriority().isBlank()) {
            ticket.setPriority("Medium");
        }
        SupportTicket saved = ticketRepository.save(ticket);

        try {
            notificationService.createCustomerNotification(
                    username,
                    "Support Inquiry Received (" + saved.getTicketNumber() + ")",
                    "Your help ticket '" + saved.getSubject() + "' has been logged. Our workshop desk will review and respond shortly.",
                    "Support",
                    "/customer/support",
                    "fa-headset"
            );
        } catch (Exception ignored) {}

        notificationService.notifyRoles("Support inquiry", saved.getTicketNumber()+" ? "+saved.getSubject(), "Support", "/dashboard/support/tickets", "MANAGER");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getTicketsForCustomer(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        return ticketRepository.findByCustomerUsernameOrderByCreatedDateDesc(username.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    @Override
    @Transactional
    public void resolveTicket(Long id, String reply, String status) {
        SupportTicket ticket = ticketRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Support ticket not found"));
        reply = com.fuelstation.util.Rules.required(reply, "Reply");
        if (reply.length() > 4000) throw new IllegalArgumentException("Reply is too long");
        if (status == null) status = "RESOLVED";
        if (!java.util.Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(status)) throw new IllegalArgumentException("Invalid ticket status");
        ticket.setAdminReply(reply); ticket.setStatus(status);
        ticket.setResolvedDate(java.util.Set.of("RESOLVED", "CLOSED").contains(status) ? LocalDateTime.now(clock) : null);
        ticketRepository.save(ticket);
        notificationService.createCustomerNotification(ticket.getCustomerUsername(), "Support ticket updated ("+ticket.getTicketNumber()+")", "Station response: "+reply, "Support", "/customer/support", "fa-headset");
    }

    @Override
    @Transactional(readOnly = true)
    public long getOpenTicketsCount() {
        return ticketRepository.countByStatus("OPEN");
    }
}
