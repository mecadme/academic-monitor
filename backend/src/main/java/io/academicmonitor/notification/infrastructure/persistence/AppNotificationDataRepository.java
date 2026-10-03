package io.academicmonitor.notification.infrastructure.persistence;

import io.academicmonitor.notification.domain.AppNotification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppNotificationDataRepository extends JpaRepository<AppNotification, UUID> {
    boolean existsByInstitutionIdAndTeacherUserIdAndEventKey(UUID institutionId, UUID teacherUserId, String eventKey);

    List<AppNotification> findByInstitutionIdAndTeacherUserIdOrderByCreatedAtDesc(
            UUID institutionId, UUID teacherUserId, Pageable pageable);

    List<AppNotification> findByInstitutionIdAndTeacherUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            UUID institutionId, UUID teacherUserId, Pageable pageable);

    long countByInstitutionIdAndTeacherUserIdAndReadAtIsNull(UUID institutionId, UUID teacherUserId);

    Optional<AppNotification> findByInstitutionIdAndTeacherUserIdAndId(UUID institutionId, UUID teacherUserId, UUID id);

    @Modifying
    @Query(
            "update AppNotification n set n.readAt = :readAt where n.institutionId = :institutionId and n.teacherUserId = :teacherUserId and n.readAt is null")
    int markAllRead(
            @Param("institutionId") UUID institutionId,
            @Param("teacherUserId") UUID teacherUserId,
            @Param("readAt") Instant readAt);
}
