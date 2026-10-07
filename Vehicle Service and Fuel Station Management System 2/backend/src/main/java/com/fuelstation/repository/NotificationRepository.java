package com.fuelstation.repository;

import com.fuelstation.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    java.util.Optional<Notification> findByIdAndRecipientUsername(Long id, String recipientUsername);
    List<Notification> findAllByOrderByCreatedDateDesc();
    long countByStatus(String status);
    List<Notification> findByRecipientUsernameOrderByCreatedDateDesc(String recipientUsername);
    long countByRecipientUsernameAndStatus(String recipientUsername, String status);
    List<Notification> findByRecipientUsernameIsNullOrderByCreatedDateDesc();
}
