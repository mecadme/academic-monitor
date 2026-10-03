package io.academicmonitor.notification.api;

import io.academicmonitor.notification.application.AppNotificationListResponse;
import io.academicmonitor.notification.application.AppNotificationResponse;
import io.academicmonitor.notification.application.AppNotificationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class AppNotificationController {
    private final AppNotificationService service;

    public AppNotificationController(AppNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public AppNotificationListResponse list(
            @RequestParam UUID institutionId,
            @RequestParam UUID teacherUserId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer limit) {
        return service.list(institutionId, teacherUserId, unreadOnly, limit);
    }

    @PostMapping("/{notificationId}/read")
    public AppNotificationResponse markRead(
            @PathVariable UUID notificationId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return service.markRead(institutionId, teacherUserId, notificationId);
    }

    @PostMapping("/read-all")
    public void markAllRead(@RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        service.markAllRead(institutionId, teacherUserId);
    }
}
