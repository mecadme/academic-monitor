package io.academicmonitor.academic.domain;

import java.util.Optional;
import java.util.UUID;

public interface AcademicPeriodRepository {

    AcademicPeriod save(AcademicPeriod academicPeriod);

    Optional<AcademicPeriod> findByAcademicYearIdAndExternalId(UUID academicYearId, String externalId);
}
