package io.academicmonitor.communication.domain;

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

    @Column(nullable = false, length = 64, updatable = false)
    private String channel;

    @Column(nullable = false, length = 64, updatable = false)
    private String provider;

    @Column(nullable = false, length = 200, updatable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunicationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
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
