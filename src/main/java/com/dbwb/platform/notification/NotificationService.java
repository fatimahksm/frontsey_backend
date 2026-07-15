package com.dbwb.platform.notification;

import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.entity.Notification;
import com.dbwb.platform.notification.entity.NotificationEvent;
import com.dbwb.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** BR-NOT-001: records the in-dashboard counterpart to a mandatory email notification. */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void notify(UUID recipientAccountId, NotificationEvent event, String message) {
        Notification notification = new Notification();
        notification.setRecipientAccountId(recipientAccountId);
        notification.setEvent(event);
        notification.setMessage(message);
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> listFor(UUID accountId) {
        return repository.findByRecipientAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID accountId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!notification.getRecipientAccountId().equals(accountId)) {
            throw new AccessDeniedForTenantException("You do not have access to this notification.");
        }
        notification.setRead(true);
    }

    @Transactional
    public void markAllAsRead(UUID accountId) {
        repository.findByRecipientAccountIdOrderByCreatedAtDesc(accountId)
                .forEach(n -> n.setRead(true));
    }
}
