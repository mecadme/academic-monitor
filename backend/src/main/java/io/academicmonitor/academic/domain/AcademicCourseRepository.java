package io.academicmonitor.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicCourseRepository {

    AcademicCourse save(AcademicCourse course);

    Optional<AcademicCourse> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    List<AcademicCourse> findByTeacherUserId(UUID teacherUserId);

    List<AcademicCourse> findByInstitutionIdAndTeacherUserId(UUID institutionId, UUID teacherUserId);
}
