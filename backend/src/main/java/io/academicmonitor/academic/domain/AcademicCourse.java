package io.academicmonitor.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "academic_courses")
public class AcademicCourse {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "platform_code", nullable = false, length = 32)
    private String platformCode;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 150)
    private String subject;

    @Column(name = "monitoring_enabled", nullable = false)
    private boolean monitoringEnabled;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AcademicCourse() {}

    public AcademicCourse(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            String externalId,
            String name,
            String subject) {
        this.institutionId = requireId(institutionId);
        this.teacherUserId = requireId(teacherUserId);
        this.platformCode = requireText(platformCode, "platformCode");
        this.externalId = requireText(externalId, "externalId");
        this.name = requireText(name, "name");
        this.subject = subject == null ? null : subject.trim();
    }

    public void enableMonitoring() {
        monitoringEnabled = true;
    }

    public void disableMonitoring() {
        monitoringEnabled = false;
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

    public String getPlatformCode() {
        return platformCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public boolean isActive() {
        return active;
    }

    private static UUID requireId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("id is required");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
