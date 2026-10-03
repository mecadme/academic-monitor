package io.academicmonitor.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppNotificationRepository {
    AppNotification save(AppNotification notification);

    boolean existsByOwnerAndEventKey(UUID institutionId, UUID teacherUserId, String eventKey);

    List<AppNotification> findByOwner(UUID institutionId, UUID teacherUserId, boolean unreadOnly, int limit);

    long countUnread(UUID institutionId, UUID teacherUserId);

    Optional<AppNotification> findByOwnerAndId(UUID institutionId, UUID teacherUserId, UUID id);

    int markAllRead(UUID institutionId, UUID teacherUserId, Instant readAt);
}
