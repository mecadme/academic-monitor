package io.academicmonitor.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CourseEnrollment() {}

    public CourseEnrollment(UUID courseId, UUID studentId) {
        if (courseId == null || studentId == null) {
            throw new IllegalArgumentException("courseId and studentId are required");
        }

        this.courseId = courseId;
        this.studentId = studentId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public boolean isActive() {
        return active;
    }
}
