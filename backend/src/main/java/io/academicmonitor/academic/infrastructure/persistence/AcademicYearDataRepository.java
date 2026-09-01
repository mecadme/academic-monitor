package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.AcademicYear;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AcademicYearDataRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);
}
