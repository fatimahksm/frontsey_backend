package com.dbwb.platform.notification;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.notification.dto.NotificationResponse;
import com.dbwb.platform.security.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** BRD 9.15: dashboard notification bell for the authenticated account. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentAccount currentAccount;

    public NotificationController(NotificationService notificationService, CurrentAccount currentAccount) {
        this.notificationService = notificationService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        return ApiResponse.ok(notificationService.listFor(currentAccount.get().accountId())
                .stream().map(NotificationResponse::from).toList());
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id, currentAccount.get().accountId());
        return ApiResponse.ok(null, "Notification marked as read.");
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentAccount.get().accountId());
        return ApiResponse.ok(null, "All notifications marked as read.");
    }
}
