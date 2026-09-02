package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.AcademicCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AcademicCourseDataRepository extends JpaRepository<AcademicCourse, UUID> {

    Optional<AcademicCourse> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    List<AcademicCourse> findByTeacherUserId(UUID teacherUserId);

    List<AcademicCourse> findByInstitutionIdAndTeacherUserId(UUID institutionId, UUID teacherUserId);
}
