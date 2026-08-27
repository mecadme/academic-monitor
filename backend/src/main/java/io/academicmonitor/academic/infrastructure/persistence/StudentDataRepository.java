package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.Student;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StudentDataRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);
}
