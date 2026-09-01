package io.academicmonitor.academic.application;

import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AcademicCalendarSynchronizer {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    public AcademicCalendarSynchronizer(
            AcademicYearRepository academicYearRepository, AcademicPeriodRepository academicPeriodRepository) {
        this.academicYearRepository = academicYearRepository;
        this.academicPeriodRepository = academicPeriodRepository;
    }

    Result synchronize(UUID institutionId, String platformCode, PlatformAcademicYearSnapshot platformAcademicYear) {
        if (platformAcademicYear == null) {
            throw new IllegalStateException("Course snapshot did not contain an academic year");
        }

        AcademicYear academicYear = academicYearRepository
                .findByInstitutionIdAndPlatformCodeAndExternalId(
                        institutionId, platformCode, platformAcademicYear.externalId())
                .orElseGet(() -> academicYearRepository.save(new AcademicYear(
                        institutionId,
                        platformCode,
                        platformAcademicYear.externalId(),
                        platformAcademicYear.name(),
                        platformAcademicYear.year(),
                        platformAcademicYear.baseScore())));

        Map<String, UUID> periodIdsByExternalId = new LinkedHashMap<>();

        for (PlatformAcademicPeriodSnapshot platformPeriod : platformAcademicYear.periods()) {
            if (periodIdsByExternalId.containsKey(platformPeriod.externalId())) {
                throw new IllegalStateException(
                        "Academic year contains duplicate period external id: " + platformPeriod.externalId());
            }

            AcademicPeriod academicPeriod = academicPeriodRepository
                    .findByAcademicYearIdAndExternalId(academicYear.getId(), platformPeriod.externalId())
                    .orElseGet(() -> academicPeriodRepository.save(new AcademicPeriod(
                            academicYear.getId(),
                            platformPeriod.externalId(),
                            platformPeriod.name(),
                            platformPeriod.abbreviation(),
                            platformPeriod.order())));

            periodIdsByExternalId.put(platformPeriod.externalId(), academicPeriod.getId());
        }

        return new Result(academicYear.getId(), periodIdsByExternalId);
    }

    record Result(UUID academicYearId, Map<String, UUID> periodIdsByExternalId) {

        Result {
            if (academicYearId == null) {
                throw new IllegalArgumentException("academicYearId is required");
            }
            periodIdsByExternalId = periodIdsByExternalId == null ? Map.of() : Map.copyOf(periodIdsByExternalId);
        }
    }
}
