package io.academicmonitor.notification.application;

import java.util.UUID;

public class AppNotificationNotFoundException extends RuntimeException {
    public AppNotificationNotFoundException(UUID notificationId) {
        super("App notification was not found: " + notificationId);
    }
}
