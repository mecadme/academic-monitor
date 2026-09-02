package io.academicmonitor.academic.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicPeriodRepository {

    AcademicPeriod save(AcademicPeriod academicPeriod);

    Optional<AcademicPeriod> findByAcademicYearIdAndExternalId(UUID academicYearId, String externalId);

    List<AcademicPeriod> findByAcademicYearIdIn(Collection<UUID> academicYearIds);
}
