package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcademicPeriodCatalogQueryServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID YEAR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID T1_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID T2_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void returnsOnlyScopedPeriodsInStableOrderAndMarksScopedActivityPeriodsAsSynchronized() {
        AcademicCourseRepository courseRepository = mock(AcademicCourseRepository.class);
        AcademicYearRepository yearRepository = mock(AcademicYearRepository.class);
        AcademicPeriodRepository periodRepository = mock(AcademicPeriodRepository.class);
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        AcademicPeriodCatalogQueryService service = new AcademicPeriodCatalogQueryService(
                courseRepository, yearRepository, periodRepository, activityRepository);

        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getAcademicYearId()).thenReturn(YEAR_ID);
        AcademicYear year = mock(AcademicYear.class);
        when(year.getId()).thenReturn(YEAR_ID);
        when(year.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(year.getName()).thenReturn("2025 - 2026");
        AcademicPeriod t1 = period(T1_ID, YEAR_ID, "Primer trimestre", "T1", 1);
        AcademicPeriod t2 = period(T2_ID, YEAR_ID, "Segundo trimestre", "T2", 2);
        AcademicPeriod foreignPeriod = period(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                "Foreign period",
                "FP",
                1);
        Activity synchronizedActivity = mock(Activity.class);
        when(synchronizedActivity.getCourseId()).thenReturn(COURSE_ID);
        when(synchronizedActivity.getAcademicPeriodId()).thenReturn(T2_ID);
        Activity foreignActivity = mock(Activity.class);
        when(foreignActivity.getCourseId()).thenReturn(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        when(foreignActivity.getAcademicPeriodId()).thenReturn(T1_ID);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of(course));
        when(yearRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(YEAR_ID)))
                .thenReturn(List.of(year));
        when(periodRepository.findByAcademicYearIdIn(Set.of(YEAR_ID))).thenReturn(List.of(t2, foreignPeriod, t1));
        when(activityRepository.findActivitiesByCourseIdIn(Set.of(COURSE_ID)))
                .thenReturn(List.of(foreignActivity, synchronizedActivity));

        AcademicPeriodCatalogResponse result = service.getPeriods(INSTITUTION_ID, TEACHER_ID);

        assertEquals(INSTITUTION_ID, result.institutionId());
        assertEquals(TEACHER_ID, result.teacherUserId());
        assertEquals(
                List.of(T1_ID, T2_ID),
                result.periods().stream()
                        .map(AcademicPeriodCatalogResponse.AcademicPeriodItem::id)
                        .toList());
        assertFalse(result.periods().getFirst().synchronizedPeriod());
        assertTrue(result.periods().get(1).synchronizedPeriod());
    }

    private static AcademicPeriod period(UUID id, UUID academicYearId, String name, String abbreviation, int order) {
        AcademicPeriod period = mock(AcademicPeriod.class);
        when(period.getId()).thenReturn(id);
        when(period.getAcademicYearId()).thenReturn(academicYearId);
        when(period.getName()).thenReturn(name);
        when(period.getAbbreviation()).thenReturn(abbreviation);
        when(period.getOrder()).thenReturn(order);
        return period;
    }
}
