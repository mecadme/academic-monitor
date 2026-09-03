package io.academicmonitor.monitoring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "score_snapshot", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    protected Alert() {}

    public Alert(
            UUID institutionId,
            UUID courseId,
            UUID activityId,
            UUID studentId,
            String ruleCode,
            AlertSeverity severity,
            BigDecimal scoreSnapshot) {
        this.institutionId = institutionId;
        this.courseId = courseId;
        this.activityId = activityId;
        this.studentId = studentId;
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.scoreSnapshot = scoreSnapshot;
    }

    public void resolve() {
        if (status == AlertStatus.RESOLVED) {
            return;
        }

        status = AlertStatus.RESOLVED;
        resolvedAt = Instant.now();
    }

    public boolean acknowledge() {
        if (!isOpen() || acknowledgedAt != null) {
            return false;
        }

        acknowledgedAt = Instant.now();
        return true;
    }

    public boolean markPending() {
        if (!isOpen() || acknowledgedAt == null) {
            return false;
        }

        acknowledgedAt = null;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public BigDecimal getScoreSnapshot() {
        return scoreSnapshot;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public boolean isOpen() {
        return status == AlertStatus.OPEN;
    }

    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    public boolean isPending() {
        return isOpen() && acknowledgedAt == null;
    }

    public void refresh(AlertSeverity severity, BigDecimal scoreSnapshot) {
        if (severity == null || scoreSnapshot == null) {
            throw new IllegalArgumentException("severity and scoreSnapshot are required");
        }

        if (isOpen() && this.severity == AlertSeverity.WARNING && severity == AlertSeverity.CRITICAL) {
            acknowledgedAt = null;
        }

        this.severity = severity;
        this.scoreSnapshot = scoreSnapshot;
    }
}
