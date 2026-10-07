package com.fuelstation.repository;

import com.fuelstation.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCustomerUsernameOrderByCreatedDateDesc(String customerUsername);
    List<SupportTicket> findAllByOrderByCreatedDateDesc();
    long countByStatus(String status);
}
