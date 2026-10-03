package io.academicmonitor.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "app_notifications")
public class AppNotification {
    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48, updatable = false)
    private AppNotificationType type;

    @Column(nullable = false, length = 160, updatable = false)
    private String title;

    @Column(nullable = false, length = 500, updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 32, updatable = false)
    private AppNotificationReferenceType referenceType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "academic_period_id", updatable = false)
    private UUID academicPeriodId;

    @Column(name = "event_key", length = 160, updatable = false)
    private String eventKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected AppNotification() {}

    public AppNotification(
            UUID institutionId,
            UUID teacherUserId,
            AppNotificationType type,
            String title,
            String message,
            AppNotificationReferenceType referenceType,
            UUID referenceId,
            UUID academicPeriodId,
            String eventKey) {
        this.institutionId = require(institutionId, "institutionId");
        this.teacherUserId = require(teacherUserId, "teacherUserId");
        this.type = require(type, "type");
        this.title = requireText(title, "title");
        this.message = requireText(message, "message");
        this.referenceType = require(referenceType, "referenceType");
        if ((referenceType == AppNotificationReferenceType.ALERT
                        || referenceType == AppNotificationReferenceType.COMMUNICATION)
                && referenceId == null) throw new IllegalArgumentException("referenceId is required");
        this.referenceId = referenceId;
        this.academicPeriodId = academicPeriodId;
        this.eventKey = eventKey == null || eventKey.isBlank() ? null : eventKey.trim();
    }

    public boolean markRead(Instant now) {
        if (readAt != null) return false;
        readAt = now == null ? Instant.now() : now;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public UUID getTeacherUserId() {
        return teacherUserId;
    }

    public AppNotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public AppNotificationReferenceType getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
