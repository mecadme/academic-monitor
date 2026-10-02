package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformGradeSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.Grade;
import io.academicmonitor.academic.domain.GradeRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.monitoring.application.AlertEvaluationService;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
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
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
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

    @Test
    void firstSyncCreatesExactlyOneGrade() {
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);

        fixture.synchronize(activity("activity-001", grade("student-001", "6.00", "2026-09-01T12:00:00Z")));

        assertEquals(1, fixture.grades().size());
        assertEquals(
                new BigDecimal("6.00"), fixture.grade(ACTIVITY_ID, STUDENT_ID).getScore());
    }

    @Test
    void identicalSecondSyncKeepsTheSameGrade() {
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);
        PlatformActivitySnapshot activity =
                activity("activity-001", grade("student-001", "6.00", "2026-09-01T12:00:00Z"));

        fixture.synchronize(activity);
        Grade original = fixture.grade(ACTIVITY_ID, STUDENT_ID);

        fixture.synchronize(activity);

        assertEquals(1, fixture.grades().size());
        assertSame(original, fixture.grade(ACTIVITY_ID, STUDENT_ID));
    }

    @Test
    void existingGradeIsUpdatedInPlaceWhenItsScoreChanges() {
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);

        fixture.synchronize(activity("activity-001", grade("student-001", "6.00", "2026-09-01T12:00:00Z")));
        Grade original = fixture.grade(ACTIVITY_ID, STUDENT_ID);

        fixture.synchronize(activity("activity-001", grade("student-001", "8.00", "2026-09-02T12:00:00Z")));

        Grade updated = fixture.grade(ACTIVITY_ID, STUDENT_ID);
        assertEquals(1, fixture.grades().size());
        assertSame(original, updated);
        assertEquals(new BigDecimal("8.00"), updated.getScore());
        assertEquals(Instant.parse("2026-09-02T12:00:00Z"), updated.getSourceUpdatedAt());
    }

    @Test
    void unchangedExistingGradeIsIdempotent() {
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);
        PlatformActivitySnapshot activity =
                activity("activity-001", grade("student-001", "8.00", "2026-09-01T12:00:00Z"));

        fixture.synchronize(activity);
        Grade original = fixture.grade(ACTIVITY_ID, STUDENT_ID);
        fixture.synchronize(activity);

        assertSame(original, fixture.grade(ACTIVITY_ID, STUDENT_ID));
        assertEquals(new BigDecimal("8.00"), original.getScore());
    }

    @Test
    void createsOneGradePerActivityAndStudentNaturalKey() {
        UUID secondStudentId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID secondActivityId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerStudent("student-002", secondStudentId);
        fixture.registerActivity("activity-001", ACTIVITY_ID);
        fixture.registerActivity("activity-002", secondActivityId);

        fixture.synchronize(
                activity(
                        "activity-001",
                        grade("student-001", "6.00", "2026-09-01T12:00:00Z"),
                        grade("student-002", "7.00", "2026-09-01T12:00:00Z")),
                activity("activity-002", grade("student-001", "8.00", "2026-09-01T12:00:00Z")));

        assertEquals(3, fixture.grades().size());
        assertEquals(
                new BigDecimal("6.00"), fixture.grade(ACTIVITY_ID, STUDENT_ID).getScore());
        assertEquals(
                new BigDecimal("7.00"),
                fixture.grade(ACTIVITY_ID, secondStudentId).getScore());
        assertEquals(
                new BigDecimal("8.00"),
                fixture.grade(secondActivityId, STUDENT_ID).getScore());
    }

    @Test
    void duplicateGradeEntriesInOneSnapshotAreConsolidatedBeforePersistence() {
        GradeSyncFixture fixture = new GradeSyncFixture();
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);

        fixture.synchronize(activity(
                "activity-001",
                grade("student-001", "6.00", "2026-09-01T12:00:00Z"),
                grade("student-001", "8.00", "2026-09-02T12:00:00Z")));

        assertEquals(1, fixture.grades().size());
        assertEquals(
                new BigDecimal("8.00"), fixture.grade(ACTIVITY_ID, STUDENT_ID).getScore());
        verify(fixture.alertEvaluationService, times(1))
                .evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("8.00"));
    }

    @Test
    void updatingAnExistingGradePreservesTheAlertLifecycle() {
        InMemoryAlertRepository alertRepository = new InMemoryAlertRepository();
        GradeSyncFixture fixture = new GradeSyncFixture(new AlertEvaluationService(alertRepository));
        fixture.registerStudent("student-001", STUDENT_ID);
        fixture.registerActivity("activity-001", ACTIVITY_ID);

        fixture.synchronize(activity("activity-001", grade("student-001", "6.00", "2026-09-01T12:00:00Z")));
        Grade original = fixture.grade(ACTIVITY_ID, STUDENT_ID);
        assertEquals(AlertStatus.OPEN, alertRepository.alerts().getFirst().getStatus());

        fixture.synchronize(activity("activity-001", grade("student-001", "8.00", "2026-09-02T12:00:00Z")));

        assertSame(original, fixture.grade(ACTIVITY_ID, STUDENT_ID));
        assertEquals(1, alertRepository.alerts().size());
        assertEquals(AlertStatus.RESOLVED, alertRepository.alerts().getFirst().getStatus());
    }

    private static PlatformCourseSnapshot platformCourse(String periodExternalId) {
        PlatformActivitySnapshot activity = new PlatformActivitySnapshot(
                "activity-001", "Activity", BigDecimal.TEN, LocalDate.of(2026, 9, 25), periodExternalId, List.of());

        return new PlatformCourseSnapshot("course-001", "Course", "Physics", null, List.of(activity), List.of());
    }

    private static PlatformActivitySnapshot activity(String externalId, PlatformGradeSnapshot... grades) {
        return new PlatformActivitySnapshot(
                externalId, "Activity", BigDecimal.TEN, LocalDate.of(2026, 9, 25), null, List.of(grades));
    }

    private static PlatformGradeSnapshot grade(String studentExternalId, String score, String recordedAt) {
        return new PlatformGradeSnapshot(studentExternalId, new BigDecimal(score), Instant.parse(recordedAt));
    }

    private static final class GradeSyncFixture {

        private final StudentRepository studentRepository = mock(StudentRepository.class);
        private final ActivityRepository activityRepository = mock(ActivityRepository.class);
        private final GradeRepository gradeRepository = mock(GradeRepository.class);
        private final AlertEvaluationService alertEvaluationService;
        private final AcademicCourse course = mock(AcademicCourse.class);
        private final Map<GradeKey, Grade> grades = new LinkedHashMap<>();
        private final ActivityGradeSynchronizer synchronizer;

        private GradeSyncFixture() {
            this(mock(AlertEvaluationService.class));
        }

        private GradeSyncFixture(AlertEvaluationService alertEvaluationService) {
            this.alertEvaluationService = alertEvaluationService;
            when(course.getId()).thenReturn(COURSE_ID);
            when(gradeRepository.findByActivityIdAndStudentId(any(UUID.class), any(UUID.class)))
                    .thenAnswer(invocation -> Optional.ofNullable(
                            grades.get(new GradeKey(invocation.getArgument(0), invocation.getArgument(1)))));
            when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
                Grade grade = invocation.getArgument(0);
                grades.put(new GradeKey(grade.getActivityId(), grade.getStudentId()), grade);
                return grade;
            });
            synchronizer = new ActivityGradeSynchronizer(
                    studentRepository, activityRepository, gradeRepository, alertEvaluationService);
        }

        void registerStudent(String externalId, UUID studentId) {
            Student student = mock(Student.class);
            when(student.getId()).thenReturn(studentId);
            when(studentRepository.findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                            INSTITUTION_ID, PLATFORM, externalId))
                    .thenReturn(Optional.of(student));
        }

        void registerActivity(String externalId, UUID activityId) {
            Activity activity = mock(Activity.class);
            when(activity.getId()).thenReturn(activityId);
            when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, externalId))
                    .thenReturn(Optional.of(activity));
        }

        void synchronize(PlatformActivitySnapshot... activities) {
            synchronizer.synchronize(
                    INSTITUTION_ID,
                    PLATFORM,
                    course,
                    new PlatformCourseSnapshot("course-001", "Course", "Physics", null, List.of(activities), List.of()),
                    Map.of());
        }

        Map<GradeKey, Grade> grades() {
            return grades;
        }

        Grade grade(UUID activityId, UUID studentId) {
            return grades.get(new GradeKey(activityId, studentId));
        }
    }

    private static final class InMemoryAlertRepository implements AlertRepository {

        private final List<Alert> alerts = new ArrayList<>();

        @Override
        public Alert save(Alert alert) {
            if (!alerts.contains(alert)) {
                alerts.add(alert);
            }
            return alert;
        }

        @Override
        public Optional<Alert> findById(UUID alertId) {
            return Optional.empty();
        }

        @Override
        public Optional<Alert> findByActivityIdAndStudentIdAndRuleCodeAndStatus(
                UUID activityId, UUID studentId, String ruleCode, AlertStatus status) {
            return alerts.stream()
                    .filter(alert -> alert.getActivityId().equals(activityId))
                    .filter(alert -> alert.getStudentId().equals(studentId))
                    .filter(alert -> alert.getRuleCode().equals(ruleCode))
                    .filter(alert -> alert.getStatus() == status)
                    .findFirst();
        }

        @Override
        public List<Alert> findByCourseIdAndStatus(UUID courseId, AlertStatus status) {
            return alerts.stream()
                    .filter(alert -> alert.getCourseId().equals(courseId))
                    .filter(alert -> alert.getStatus() == status)
                    .toList();
        }

        @Override
        public List<Alert> findByCourseIdAndStatusAndActivityIdIn(
                UUID courseId, AlertStatus status, Collection<UUID> activityIds) {
            return alerts.stream()
                    .filter(alert -> alert.getCourseId().equals(courseId))
                    .filter(alert -> alert.getStatus() == status)
                    .filter(alert -> activityIds.contains(alert.getActivityId()))
                    .toList();
        }

        @Override
        public List<Alert> findByInstitutionIdAndCourseIdInAndStatus(
                UUID institutionId, Collection<UUID> courseIds, AlertStatus status) {
            return alerts.stream()
                    .filter(alert -> alert.getInstitutionId().equals(institutionId))
                    .filter(alert -> courseIds.contains(alert.getCourseId()))
                    .filter(alert -> alert.getStatus() == status)
                    .toList();
        }

        List<Alert> alerts() {
            return alerts;
        }
    }

    private record GradeKey(UUID activityId, UUID studentId) {}
}
