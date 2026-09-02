package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.AcademicPeriod;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AcademicPeriodDataRepository extends JpaRepository<AcademicPeriod, UUID> {

    Optional<AcademicPeriod> findByAcademicYearIdAndExternalId(UUID academicYearId, String externalId);

    List<AcademicPeriod> findByAcademicYearIdIn(Collection<UUID> academicYearIds);
}
