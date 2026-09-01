package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarSynchronizerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FIRST_PERIOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SECOND_PERIOD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String PLATFORM = "TEST";

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Test
    void synchronizesAcademicYearAndPeriodsIdempotently() {
        AcademicYear persistedYear = mock(AcademicYear.class);
        AcademicPeriod persistedFirstPeriod = mock(AcademicPeriod.class);
        AcademicPeriod persistedSecondPeriod = mock(AcademicPeriod.class);

        when(persistedYear.getId()).thenReturn(ACADEMIC_YEAR_ID);
        when(persistedFirstPeriod.getId()).thenReturn(FIRST_PERIOD_ID);
        when(persistedSecondPeriod.getId()).thenReturn(SECOND_PERIOD_ID);

        when(academicYearRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "year-2026"))
                .thenReturn(Optional.empty(), Optional.of(persistedYear));
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(persistedYear);

        when(academicPeriodRepository.findByAcademicYearIdAndExternalId(ACADEMIC_YEAR_ID, "period-first"))
                .thenReturn(Optional.empty(), Optional.of(persistedFirstPeriod));
        when(academicPeriodRepository.findByAcademicYearIdAndExternalId(ACADEMIC_YEAR_ID, "period-second"))
                .thenReturn(Optional.empty(), Optional.of(persistedSecondPeriod));
        when(academicPeriodRepository.save(any(AcademicPeriod.class)))
                .thenReturn(persistedFirstPeriod, persistedSecondPeriod);

        AcademicCalendarSynchronizer synchronizer =
                new AcademicCalendarSynchronizer(academicYearRepository, academicPeriodRepository);

        AcademicCalendarSynchronizer.Result first =
                synchronizer.synchronize(INSTITUTION_ID, PLATFORM, academicYearSnapshot());
        AcademicCalendarSynchronizer.Result second =
                synchronizer.synchronize(INSTITUTION_ID, PLATFORM, academicYearSnapshot());

        assertEquals(ACADEMIC_YEAR_ID, first.academicYearId());
        assertEquals(FIRST_PERIOD_ID, first.periodIdsByExternalId().get("period-first"));
        assertEquals(SECOND_PERIOD_ID, first.periodIdsByExternalId().get("period-second"));
        assertEquals(first, second);

        ArgumentCaptor<AcademicYear> academicYearCaptor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(academicYearCaptor.capture());

        AcademicYear createdYear = academicYearCaptor.getValue();
        assertEquals(INSTITUTION_ID, createdYear.getInstitutionId());
        assertEquals(PLATFORM, createdYear.getPlatformCode());
        assertEquals("year-2026", createdYear.getExternalId());
        assertEquals("Academic year 2026-2027", createdYear.getName());
        assertEquals("2026-2027", createdYear.getExternalYear());
        assertEquals(new BigDecimal("10.00"), createdYear.getBaseScore());

        ArgumentCaptor<AcademicPeriod> periodCaptor = ArgumentCaptor.forClass(AcademicPeriod.class);
        verify(academicPeriodRepository, times(2)).save(periodCaptor.capture());

        List<AcademicPeriod> createdPeriods = periodCaptor.getAllValues();
        assertEquals("period-first", createdPeriods.getFirst().getExternalId());
        assertEquals(1, createdPeriods.getFirst().getOrder());
        assertEquals("period-second", createdPeriods.get(1).getExternalId());
        assertEquals(2, createdPeriods.get(1).getOrder());

        assertThrows(UnsupportedOperationException.class, () -> first.periodIdsByExternalId()
                .put("another-period", UUID.randomUUID()));
    }

    private static PlatformAcademicYearSnapshot academicYearSnapshot() {
        return new PlatformAcademicYearSnapshot(
                "year-2026",
                "Academic year 2026-2027",
                "2026-2027",
                new BigDecimal("10.00"),
                List.of(
                        new PlatformAcademicPeriodSnapshot("period-first", "First period", "P1", 1),
                        new PlatformAcademicPeriodSnapshot("period-second", "Second period", "P2", 2)));
    }
}
