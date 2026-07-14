package com.dbwb.platform.notification;

import com.dbwb.platform.notification.entity.Notification;
import com.dbwb.platform.notification.entity.NotificationEvent;
import com.dbwb.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** BR-NOT-001: records the in-dashboard counterpart to a mandatory email notification. */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void notify(UUID recipientAccountId, NotificationEvent event, String message) {
        Notification notification = new Notification();
        notification.setRecipientAccountId(recipientAccountId);
        notification.setEvent(event);
        notification.setMessage(message);
        repository.save(notification);
    }

    public List<Notification> listFor(UUID accountId) {
        return repository.findByRecipientAccountIdOrderByCreatedAtDesc(accountId);
    }
}
