package io.academicmonitor.academic.domain;

import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository {

    AcademicYear save(AcademicYear academicYear);

    Optional<AcademicYear> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);
}
