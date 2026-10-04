package io.academicmonitor.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "communications")
public class Communication {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "guardian_id", nullable = false, updatable = false)
    private UUID guardianId;

    @Column(name = "alert_id", updatable = false)
    private UUID alertId;

    @Column(nullable = false, length = 64, updatable = false)
    private String channel;

    @Column(nullable = false, length = 64, updatable = false)
    private String provider;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunicationStatus status;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected Communication() {}

    public Communication(
            UUID institutionId,
            UUID teacherUserId,
            UUID studentId,
            UUID guardianId,
            String channel,
            String provider,
            String subject,
            String content) {
        this.institutionId = requireId(institutionId, "institutionId");
        this.teacherUserId = requireId(teacherUserId, "teacherUserId");
        this.studentId = requireId(studentId, "studentId");
        this.guardianId = requireId(guardianId, "guardianId");
        this.channel = requireText(channel, "channel");
        this.provider = requireText(provider, "provider");
        this.subject = requireText(subject, "subject");
        this.content = requireText(content, "content");
        status = CommunicationStatus.PENDING;
    }

    public static Communication draft(
            UUID institutionId,
            UUID teacherUserId,
            UUID studentId,
            UUID guardianId,
            UUID alertId,
            String provider,
            String subject,
            String content) {
        Communication communication = new Communication(
                institutionId,
                teacherUserId,
                studentId,
                guardianId,
                "PLATFORM_NOTIFICATION",
                provider,
                subject,
                content);
        communication.alertId = requireId(alertId, "alertId");
        communication.status = CommunicationStatus.DRAFT;
        return communication;
    }

    public void editDraft(String subject, String content) {
        if (status != CommunicationStatus.DRAFT) {
            throw new IllegalStateException("Only draft communications can be edited");
        }
        this.subject = requireText(subject, "subject");
        this.content = requireText(content, "content");
    }

    public void beginSending() {
        if (status != CommunicationStatus.DRAFT) {
            throw new IllegalStateException("Only draft communications can be sent");
        }
        status = CommunicationStatus.PENDING;
    }

    public boolean isDeletable() {
        return status == CommunicationStatus.DRAFT;
    }

    public void markSent(Instant sentAt) {
        ensurePending();
        status = CommunicationStatus.SENT;
        this.sentAt = sentAt == null ? Instant.now() : sentAt;
        failureCode = null;
        failureReason = null;
    }

    public void markFailed(String code, String reason) {
        ensurePending();
        status = CommunicationStatus.FAILED;
        failureCode = requireText(code, "failureCode");
        failureReason = requireText(reason, "failureReason");
    }

    public UUID getId() {
        return id;
    }

    public CommunicationStatus getStatus() {
        return status;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public UUID getTeacherUserId() {
        return teacherUserId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getGuardianId() {
        return guardianId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    private void ensurePending() {
        if (status != CommunicationStatus.PENDING) {
            throw new IllegalStateException("Only pending communications can be completed");
        }
    }

    private static UUID requireId(UUID value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
