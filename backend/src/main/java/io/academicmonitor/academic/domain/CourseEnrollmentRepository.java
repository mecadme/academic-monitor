package io.academicmonitor.academic.domain;

import java.util.List;
import java.util.UUID;

public interface CourseEnrollmentRepository {

    CourseEnrollment save(CourseEnrollment enrollment);

    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);

    List<CourseEnrollment> findByCourseId(UUID courseId);
}
