package io.academicmonitor.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "platform_code", nullable = false, length = 32)
    private String platformCode;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Activity() {}

    public Activity(
            UUID courseId,
            String platformCode,
            String externalId,
            String name,
            BigDecimal maxScore,
            LocalDate dueDate) {
        this.courseId = courseId;
        this.platformCode = platformCode;
        this.externalId = externalId;
        this.name = name;
        this.maxScore = maxScore;
        this.dueDate = dueDate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}
