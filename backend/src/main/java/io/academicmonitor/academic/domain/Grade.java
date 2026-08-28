package io.academicmonitor.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Grade() {}

    public Grade(UUID activityId, UUID studentId, BigDecimal score, Instant sourceUpdatedAt) {
        this.activityId = activityId;
        this.studentId = studentId;
        changeScore(score);
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public void changeScore(BigDecimal score) {
        if (score == null || score.signum() < 0) {
            throw new IllegalArgumentException("score must be zero or greater");
        }
        this.score = score;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public BigDecimal getScore() {
        return score;
    }
}
