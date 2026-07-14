package com.dbwb.platform.notification.repository;

import com.dbwb.platform.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientAccountIdOrderByCreatedAtDesc(UUID recipientAccountId);
}
