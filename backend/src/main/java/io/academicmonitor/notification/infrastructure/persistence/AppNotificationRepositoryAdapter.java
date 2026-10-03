package io.academicmonitor.notification.infrastructure.persistence;

import io.academicmonitor.notification.domain.AppNotification;
import io.academicmonitor.notification.domain.AppNotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class AppNotificationRepositoryAdapter implements AppNotificationRepository {
    private final AppNotificationDataRepository repository;

    AppNotificationRepositoryAdapter(AppNotificationDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public AppNotification save(AppNotification notification) {
        return repository.saveAndFlush(notification);
    }

    @Override
    public boolean existsByOwnerAndEventKey(UUID institutionId, UUID teacherUserId, String eventKey) {
        return repository.existsByInstitutionIdAndTeacherUserIdAndEventKey(institutionId, teacherUserId, eventKey);
    }

    @Override
    public List<AppNotification> findByOwner(UUID institutionId, UUID teacherUserId, boolean unreadOnly, int limit) {
        return unreadOnly
                ? repository.findByInstitutionIdAndTeacherUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                        institutionId, teacherUserId, PageRequest.of(0, limit))
                : repository.findByInstitutionIdAndTeacherUserIdOrderByCreatedAtDesc(
                        institutionId, teacherUserId, PageRequest.of(0, limit));
    }

    @Override
    public long countUnread(UUID institutionId, UUID teacherUserId) {
        return repository.countByInstitutionIdAndTeacherUserIdAndReadAtIsNull(institutionId, teacherUserId);
    }

    @Override
    public Optional<AppNotification> findByOwnerAndId(UUID institutionId, UUID teacherUserId, UUID id) {
        return repository.findByInstitutionIdAndTeacherUserIdAndId(institutionId, teacherUserId, id);
    }

    @Override
    public int markAllRead(UUID institutionId, UUID teacherUserId, Instant readAt) {
        return repository.markAllRead(institutionId, teacherUserId, readAt);
    }
}
