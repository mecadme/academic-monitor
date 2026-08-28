package io.academicmonitor.academic.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findStudentByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    Optional<Student> findStudentById(UUID studentId);
}
