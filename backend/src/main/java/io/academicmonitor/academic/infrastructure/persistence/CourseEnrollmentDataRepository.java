package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.CourseEnrollment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CourseEnrollmentDataRepository extends JpaRepository<CourseEnrollment, UUID> {

    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);

    List<CourseEnrollment> findByCourseId(UUID courseId);

    List<CourseEnrollment> findByCourseIdIn(Collection<UUID> courseIds);
}
