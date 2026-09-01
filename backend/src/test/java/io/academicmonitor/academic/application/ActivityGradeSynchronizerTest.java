package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.GradeRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.monitoring.application.AlertEvaluationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ActivityGradeSynchronizerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTIVITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ACADEMIC_PERIOD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_ACADEMIC_PERIOD_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String PLATFORM = "TEST";

    private StudentRepository studentRepository;
    private ActivityRepository activityRepository;
    private GradeRepository gradeRepository;
    private AlertEvaluationService alertEvaluationService;
    private AcademicCourse course;
    private ActivityGradeSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        activityRepository = mock(ActivityRepository.class);
        gradeRepository = mock(GradeRepository.class);
        alertEvaluationService = mock(AlertEvaluationService.class);
        course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(COURSE_ID);

        synchronizer = new ActivityGradeSynchronizer(
                studentRepository, activityRepository, gradeRepository, alertEvaluationService);
    }

    @Test
    void associatesNewActivityWithResolvedAcademicPeriod() {
        Activity persistedActivity = mock(Activity.class);
        when(persistedActivity.getId()).thenReturn(ACTIVITY_ID);
        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-001"))
                .thenReturn(Optional.empty());
        when(activityRepository.save(any(Activity.class))).thenReturn(persistedActivity);

        ActivityGradeSynchronizer.Result result = synchronizer.synchronize(
                INSTITUTION_ID,
                PLATFORM,
                course,
                platformCourse("period-001"),
                Map.of("period-001", ACADEMIC_PERIOD_ID));

        assertEquals(List.of(ACTIVITY_ID), result.activityIds());

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertEquals(ACADEMIC_PERIOD_ID, activityCaptor.getValue().getAcademicPeriodId());
    }

    @Test
    void enrichesLegacyActivityWithoutAcademicPeriod() {
        Activity legacyActivity = mock(Activity.class);
        when(legacyActivity.getId()).thenReturn(ACTIVITY_ID);
        when(legacyActivity.associateAcademicPeriod(ACADEMIC_PERIOD_ID)).thenReturn(true);
        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-001"))
                .thenReturn(Optional.of(legacyActivity));
        when(activityRepository.save(legacyActivity)).thenReturn(legacyActivity);

        ActivityGradeSynchronizer.Result result = synchronizer.synchronize(
                INSTITUTION_ID,
                PLATFORM,
                course,
                platformCourse("period-001"),
                Map.of("period-001", ACADEMIC_PERIOD_ID));

        assertEquals(List.of(ACTIVITY_ID), result.activityIds());
        verify(legacyActivity).associateAcademicPeriod(ACADEMIC_PERIOD_ID);
        verify(activityRepository).save(legacyActivity);
    }

    @Test
    void refusesToReassignActivityToDifferentAcademicPeriod() {
        Activity activity = new Activity(
                COURSE_ID,
                ACADEMIC_PERIOD_ID,
                PLATFORM,
                "activity-001",
                "Activity",
                BigDecimal.TEN,
                LocalDate.of(2026, 9, 25));
        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-001"))
                .thenReturn(Optional.of(activity));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> synchronizer.synchronize(
                        INSTITUTION_ID,
                        PLATFORM,
                        course,
                        platformCourse("period-002"),
                        Map.of("period-002", OTHER_ACADEMIC_PERIOD_ID)));

        assertEquals(
                "Activity activity-001 is already associated with a different academic period", exception.getMessage());
        verify(activityRepository, never()).save(activity);
    }

    @Test
    void failsExplicitlyWhenActivityReferencesUnknownAcademicPeriod() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> synchronizer.synchronize(
                        INSTITUTION_ID, PLATFORM, course, platformCourse("unknown-period"), Map.of()));

        assertEquals(
                "Activity activity-001 references an unknown academic period: unknown-period", exception.getMessage());
        verifyNoInteractions(activityRepository, studentRepository, gradeRepository, alertEvaluationService);
    }

    @Test
    void preservesUnresolvedActivityWithoutInventingAcademicPeriod() {
        Activity persistedActivity = mock(Activity.class);
        when(persistedActivity.getId()).thenReturn(ACTIVITY_ID);
        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-001"))
                .thenReturn(Optional.empty());
        when(activityRepository.save(any(Activity.class))).thenReturn(persistedActivity);

        ActivityGradeSynchronizer.Result result =
                synchronizer.synchronize(INSTITUTION_ID, PLATFORM, course, platformCourse(null), Map.of());

        assertEquals(List.of(ACTIVITY_ID), result.activityIds());

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertNull(activityCaptor.getValue().getAcademicPeriodId());
    }

    private static PlatformCourseSnapshot platformCourse(String periodExternalId) {
        PlatformActivitySnapshot activity = new PlatformActivitySnapshot(
                "activity-001", "Activity", BigDecimal.TEN, LocalDate.of(2026, 9, 25), periodExternalId, List.of());

        return new PlatformCourseSnapshot("course-001", "Course", "Physics", null, List.of(activity), List.of());
    }
}
