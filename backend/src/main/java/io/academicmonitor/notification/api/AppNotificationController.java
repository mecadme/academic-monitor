package io.academicmonitor.notification.api;

import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
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
    private final AuthenticatedAcademicContext context;
    private final AppNotificationService service;

    public AppNotificationController(AppNotificationService service, AuthenticatedAcademicContext context) {
        this.context = context;
        this.service = service;
    }

    @GetMapping
    public AppNotificationListResponse list(
            @RequestParam(defaultValue = "false") boolean unreadOnly, @RequestParam(required = false) Integer limit) {
        return service.list(context.institutionId(), context.userId(), unreadOnly, limit);
    }

    @PostMapping("/{notificationId}/read")
    public AppNotificationResponse markRead(@PathVariable UUID notificationId) {
        return service.markRead(context.institutionId(), context.userId(), notificationId);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        service.markAllRead(context.institutionId(), context.userId());
    }
}
