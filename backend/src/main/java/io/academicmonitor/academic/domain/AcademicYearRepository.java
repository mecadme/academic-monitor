package io.academicmonitor.academic.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository {

    AcademicYear save(AcademicYear academicYear);

    Optional<AcademicYear> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    List<AcademicYear> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> academicYearIds);
}
