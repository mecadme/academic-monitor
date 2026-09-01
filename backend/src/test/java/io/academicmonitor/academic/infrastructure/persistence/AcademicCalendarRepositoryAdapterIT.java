package io.academicmonitor.academic.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcademicCalendarRepositoryAdapterIT extends PostgresIntegrationTest {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Test
    void persistsAndResolvesAcademicCalendarByNeutralExternalIdentities() {
        Institution institution =
                institutionRepository.save(new Institution("Calendar Test School", "America/Guayaquil"));

        AcademicYear academicYear = academicYearRepository.save(new AcademicYear(
                institution.getId(),
                "TEST",
                "year-2026",
                "Academic year 2026-2027",
                "2026-2027",
                new BigDecimal("10.00")));

        AcademicPeriod academicPeriod = academicPeriodRepository.save(
                new AcademicPeriod(academicYear.getId(), "period-first", "First period", "P1", 1));

        assertNotNull(academicYear.getId());
        assertNotNull(academicPeriod.getId());
        assertEquals(
                academicYear.getId(),
                academicYearRepository
                        .findByInstitutionIdAndPlatformCodeAndExternalId(institution.getId(), "TEST", "year-2026")
                        .orElseThrow()
                        .getId());
        assertEquals(
                academicPeriod.getId(),
                academicPeriodRepository
                        .findByAcademicYearIdAndExternalId(academicYear.getId(), "period-first")
                        .orElseThrow()
                        .getId());
    }
}
