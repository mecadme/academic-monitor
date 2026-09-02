package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AcademicCalendarRepositoryAdapter implements AcademicYearRepository, AcademicPeriodRepository {

    private final AcademicYearDataRepository academicYearRepository;
    private final AcademicPeriodDataRepository academicPeriodRepository;

    AcademicCalendarRepositoryAdapter(
            AcademicYearDataRepository academicYearRepository, AcademicPeriodDataRepository academicPeriodRepository) {
        this.academicYearRepository = academicYearRepository;
        this.academicPeriodRepository = academicPeriodRepository;
    }

    @Override
    public AcademicYear save(AcademicYear academicYear) {
        return academicYearRepository.save(academicYear);
    }

    @Override
    public Optional<AcademicYear> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId) {
        return academicYearRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                institutionId, platformCode, externalId);
    }

    @Override
    public List<AcademicYear> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> academicYearIds) {
        return academicYearRepository.findByInstitutionIdAndIdIn(institutionId, academicYearIds);
    }

    @Override
    public AcademicPeriod save(AcademicPeriod academicPeriod) {
        return academicPeriodRepository.save(academicPeriod);
    }

    @Override
    public Optional<AcademicPeriod> findByAcademicYearIdAndExternalId(UUID academicYearId, String externalId) {
        return academicPeriodRepository.findByAcademicYearIdAndExternalId(academicYearId, externalId);
    }
}
