package io.academicmonitor.notification.application;

import io.academicmonitor.notification.domain.AppNotification;
import io.academicmonitor.notification.domain.AppNotificationReferenceType;
import io.academicmonitor.notification.domain.AppNotificationType;
import java.time.Instant;
import java.util.UUID;

public record AppNotificationResponse(
        UUID id,
        AppNotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Reference reference) {
    public static AppNotificationResponse from(AppNotification notification) {
        return new AppNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                new Reference(notification.getReferenceType(), notification.getReferenceId()));
    }

    public record Reference(AppNotificationReferenceType type, UUID id) {}
}
