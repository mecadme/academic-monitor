package io.academicmonitor.monitoring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertInboxQueryServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID TEACHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_A_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COURSE_B_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("33333333-3333-3333-3333-333333333334");
    private static final UUID PERIOD_T1_ID = UUID.fromString("33333333-3333-3333-3333-333333333335");
    private static final UUID PERIOD_T2_ID = UUID.fromString("33333333-3333-3333-3333-333333333336");
    private static final UUID FOREIGN_INSTITUTION_COURSE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID FOREIGN_TEACHER_COURSE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ACTIVITY_A_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ACTIVITY_B_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID STUDENT_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID STUDENT_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ALERT_CRITICAL_LOW_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ALERT_CRITICAL_HIGH_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ALERT_WARNING_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private AcademicCourseRepository courseRepository;
    private AcademicPeriodRepository academicPeriodRepository;
    private AlertRepository alertRepository;
    private ActivityRepository activityRepository;
    private StudentRepository studentRepository;
    private AlertInboxQueryService service;

    @BeforeEach
    void setUp() {
        courseRepository = mock(AcademicCourseRepository.class);
        academicPeriodRepository = mock(AcademicPeriodRepository.class);
        alertRepository = mock(AlertRepository.class);
        activityRepository = mock(ActivityRepository.class);
        studentRepository = mock(StudentRepository.class);
        service = new AlertInboxQueryService(
                courseRepository, academicPeriodRepository, alertRepository, activityRepository, studentRepository);
    }

    @Test
    void returnsEmptyInboxWithoutChildQueriesWhenTeacherHasNoCourses() {
        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of());

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null);

        assertEquals(INSTITUTION_ID, result.institutionId());
        assertEquals(TEACHER_USER_ID, result.teacherUserId());
        assertEquals(0, result.total());
        assertEquals(List.of(), result.alerts());
        verifyNoInteractions(alertRepository, activityRepository, studentRepository);
    }

    @Test
    void returnsEmptyInboxWithoutEnrichmentQueriesWhenThereAreNoOpenAlerts() {
        AcademicCourse course = course(COURSE_A_ID, "Course A", "Physics");
        Set<UUID> courseIds = Set.of(COURSE_A_ID);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(course));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, courseIds, AlertStatus.OPEN))
                .thenReturn(List.of());

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null);

        assertEquals(0, result.total());
        assertEquals(List.of(), result.alerts());
        verifyNoInteractions(activityRepository, studentRepository);
    }

    @Test
    void returnsOnlyScopedOpenEnrichedAlertsSortedByUrgencyWithoutDuplicates() {
        AcademicCourse courseA = course(COURSE_A_ID, "Primer Curso A, Bachillerato General Unificado", "Física");
        AcademicCourse courseB = course(COURSE_B_ID, "Segundo Curso B, Bachillerato General Unificado", "Química");
        Set<UUID> courseIds = Set.of(COURSE_A_ID, COURSE_B_ID);

        Activity activityA = activity(ACTIVITY_A_ID, COURSE_A_ID, "Movimiento rectilíneo", "2026-01-15");
        Activity activityB = activity(ACTIVITY_B_ID, COURSE_B_ID, "Enlaces químicos", "2026-02-20");
        Student studentA = student(STUDENT_A_ID, INSTITUTION_ID, "Ana Torres");
        Student studentB = student(STUDENT_B_ID, INSTITUTION_ID, "Bruno Vega");

        Alert criticalLow = alert(
                ALERT_CRITICAL_LOW_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "3.50",
                true);
        Alert criticalHigh = alert(
                ALERT_CRITICAL_HIGH_ID,
                INSTITUTION_ID,
                COURSE_B_ID,
                ACTIVITY_B_ID,
                STUDENT_B_ID,
                AlertSeverity.CRITICAL,
                "4.50",
                true);
        Alert warning = alert(
                ALERT_WARNING_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_B_ID,
                AlertSeverity.WARNING,
                "2.00",
                true);
        Alert resolved = alert(
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.WARNING,
                "6.50",
                false);
        Alert foreignInstitution = alert(
                UUID.fromString("12121212-1212-1212-1212-121212121212"),
                OTHER_INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "1.00",
                true);
        Alert foreignInstitutionCourse = alert(
                UUID.fromString("13131313-1313-1313-1313-131313131313"),
                OTHER_INSTITUTION_ID,
                FOREIGN_INSTITUTION_COURSE_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "1.00",
                true);
        Alert foreignTeacherCourse = alert(
                UUID.fromString("14141414-1414-1414-1414-141414141414"),
                INSTITUTION_ID,
                FOREIGN_TEACHER_COURSE_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "1.00",
                true);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(courseB, courseA));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, courseIds, AlertStatus.OPEN))
                .thenReturn(List.of(
                        warning,
                        criticalHigh,
                        resolved,
                        foreignInstitution,
                        foreignInstitutionCourse,
                        foreignTeacherCourse,
                        criticalLow,
                        criticalHigh));
        when(activityRepository.findActivitiesByCourseIdIn(courseIds)).thenReturn(List.of(activityA, activityB));
        when(studentRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID, STUDENT_B_ID)))
                .thenReturn(List.of(studentA, studentB));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null);

        assertEquals(3, result.total());
        assertEquals(
                List.of(ALERT_CRITICAL_LOW_ID, ALERT_CRITICAL_HIGH_ID, ALERT_WARNING_ID),
                result.alerts().stream().map(AlertInboxResponse.AlertItem::id).toList());

        AlertInboxResponse.AlertItem first = result.alerts().getFirst();
        assertEquals(AlertSeverity.CRITICAL, first.severity());
        assertEquals("LOW_GRADE", first.ruleCode());
        assertEquals(new BigDecimal("3.50"), first.score());
        assertEquals(COURSE_A_ID, first.course().id());
        assertEquals(
                "Primer Curso A, Bachillerato General Unificado", first.course().name());
        assertEquals("Física", first.course().subject());
        assertEquals(ACTIVITY_A_ID, first.activity().id());
        assertEquals("Movimiento rectilíneo", first.activity().name());
        assertEquals(new BigDecimal("10.00"), first.activity().maximumScore());
        assertEquals(LocalDate.of(2026, 1, 15), first.activity().dueDate());
        assertEquals(STUDENT_A_ID, first.student().id());
        assertEquals("Ana Torres", first.student().name());

        verify(alertRepository).findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, courseIds, AlertStatus.OPEN);
        verify(activityRepository).findActivitiesByCourseIdIn(courseIds);
        verify(studentRepository).findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID, STUDENT_B_ID));
    }

    @Test
    void filtersByOwnedCourse() {
        AcademicCourse courseA = course(COURSE_A_ID, "Course A", "Physics");
        AcademicCourse courseB = course(COURSE_B_ID, "Course B", "Chemistry");
        Activity activity = activity(ACTIVITY_A_ID, COURSE_A_ID, "Activity A", "2026-01-15");
        Student student = student(STUDENT_A_ID, INSTITUTION_ID, "Ana Torres");
        Alert alert = alert(
                ALERT_CRITICAL_LOW_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "4.50",
                true);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(courseA, courseB));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(
                        INSTITUTION_ID, Set.of(COURSE_A_ID), AlertStatus.OPEN))
                .thenReturn(List.of(alert));
        when(activityRepository.findActivitiesByCourseIdIn(Set.of(COURSE_A_ID))).thenReturn(List.of(activity));
        when(studentRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID)))
                .thenReturn(List.of(student));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_A_ID);

        assertEquals(1, result.total());
        assertEquals(COURSE_A_ID, result.alerts().getFirst().course().id());
        verify(alertRepository)
                .findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, Set.of(COURSE_A_ID), AlertStatus.OPEN);
    }

    @Test
    void returnsEmptyInboxForUnownedCourseFilterWithoutLeakingItsExistence() {
        AcademicCourse course = course(COURSE_A_ID, "Course A", "Physics");

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(course));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, FOREIGN_TEACHER_COURSE_ID);

        assertEquals(0, result.total());
        assertEquals(List.of(), result.alerts());
        verifyNoInteractions(alertRepository, activityRepository, studentRepository);
    }

    @Test
    void filtersAlertsByOwnedAcademicPeriodAndExcludesOtherOrUnassignedActivities() {
        AcademicCourse course = course(COURSE_A_ID, "Course A", "Physics");
        AcademicPeriod periodT1 = period(PERIOD_T1_ID);
        Activity t1Activity = activity(ACTIVITY_A_ID, COURSE_A_ID, PERIOD_T1_ID, "T1 Activity", "2026-01-15");
        Activity t2Activity = activity(ACTIVITY_B_ID, COURSE_A_ID, PERIOD_T2_ID, "T2 Activity", "2026-02-15");
        UUID unassignedActivityId = UUID.fromString("89898989-8989-8989-8989-898989898989");
        Activity unassignedActivity =
                activity(unassignedActivityId, COURSE_A_ID, null, "Legacy Activity", "2026-03-15");
        Student student = student(STUDENT_A_ID, INSTITUTION_ID, "Ana Torres");
        Alert t1Alert = alert(
                ALERT_CRITICAL_LOW_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "3.50",
                true);
        Alert t2Alert = alert(
                ALERT_CRITICAL_HIGH_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_B_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "4.50",
                true);
        Alert unassignedAlert = alert(
                ALERT_WARNING_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                unassignedActivityId,
                STUDENT_A_ID,
                AlertSeverity.WARNING,
                "6.50",
                true);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(course));
        when(academicPeriodRepository.findByAcademicYearIdIn(Set.of(ACADEMIC_YEAR_ID)))
                .thenReturn(List.of(periodT1));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(
                        INSTITUTION_ID, Set.of(COURSE_A_ID), AlertStatus.OPEN))
                .thenReturn(List.of(t2Alert, unassignedAlert, t1Alert));
        when(activityRepository.findActivitiesByCourseIdIn(Set.of(COURSE_A_ID)))
                .thenReturn(List.of(t1Activity, t2Activity, unassignedActivity));
        when(studentRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID)))
                .thenReturn(List.of(student));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null, PERIOD_T1_ID);

        assertEquals(1, result.total());
        assertEquals(
                List.of(ALERT_CRITICAL_LOW_ID),
                result.alerts().stream().map(AlertInboxResponse.AlertItem::id).toList());
    }

    @Test
    void keepsAlertsFromEveryPeriodWhenNoPeriodFilterIsSupplied() {
        AcademicCourse course = course(COURSE_A_ID, "Course A", "Physics");
        Activity t1Activity = activity(ACTIVITY_A_ID, COURSE_A_ID, PERIOD_T1_ID, "T1 Activity", "2026-01-15");
        Activity t2Activity = activity(ACTIVITY_B_ID, COURSE_A_ID, PERIOD_T2_ID, "T2 Activity", "2026-02-15");
        Student student = student(STUDENT_A_ID, INSTITUTION_ID, "Ana Torres");
        Alert t1Alert = alert(
                ALERT_CRITICAL_LOW_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_A_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "3.50",
                true);
        Alert t2Alert = alert(
                ALERT_CRITICAL_HIGH_ID,
                INSTITUTION_ID,
                COURSE_A_ID,
                ACTIVITY_B_ID,
                STUDENT_A_ID,
                AlertSeverity.CRITICAL,
                "4.50",
                true);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(course));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(
                        INSTITUTION_ID, Set.of(COURSE_A_ID), AlertStatus.OPEN))
                .thenReturn(List.of(t2Alert, t1Alert));
        when(activityRepository.findActivitiesByCourseIdIn(Set.of(COURSE_A_ID)))
                .thenReturn(List.of(t1Activity, t2Activity));
        when(studentRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID)))
                .thenReturn(List.of(student));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null, null);

        assertEquals(2, result.total());
        assertEquals(
                List.of(ALERT_CRITICAL_LOW_ID, ALERT_CRITICAL_HIGH_ID),
                result.alerts().stream().map(AlertInboxResponse.AlertItem::id).toList());
        verifyNoInteractions(academicPeriodRepository);
    }

    @Test
    void returnsEmptyInboxForUnownedAcademicPeriodWithoutQueryingAlerts() {
        AcademicCourse course = course(COURSE_A_ID, "Course A", "Physics");
        AcademicPeriod periodT1 = period(PERIOD_T1_ID);

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(course));
        when(academicPeriodRepository.findByAcademicYearIdIn(Set.of(ACADEMIC_YEAR_ID)))
                .thenReturn(List.of(periodT1));

        AlertInboxResponse result = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null, PERIOD_T2_ID);

        assertEquals(0, result.total());
        assertEquals(List.of(), result.alerts());
        verifyNoInteractions(alertRepository, activityRepository, studentRepository);
    }

    @Test
    void attentionStateFiltersComposeWithOwnedCourseAndAcademicPeriod() {
        AcademicCourse courseA = course(COURSE_A_ID, "Course A", "Physics");
        AcademicCourse courseB = course(COURSE_B_ID, "Course B", "Chemistry");
        AcademicPeriod periodT1 = period(PERIOD_T1_ID);
        Activity activity = activity(ACTIVITY_A_ID, COURSE_A_ID, PERIOD_T1_ID, "T1 Activity", "2026-01-15");
        Student student = student(STUDENT_A_ID, INSTITUTION_ID, "Synthetic Student");
        Alert pending = attentionAlert(ALERT_CRITICAL_LOW_ID, ACTIVITY_A_ID, AlertSeverity.CRITICAL, null);
        Alert acknowledged = attentionAlert(
                ALERT_WARNING_ID, ACTIVITY_A_ID, AlertSeverity.WARNING, Instant.parse("2026-09-02T12:00:00Z"));

        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(courseA, courseB));
        when(academicPeriodRepository.findByAcademicYearIdIn(Set.of(ACADEMIC_YEAR_ID)))
                .thenReturn(List.of(periodT1));
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(
                        INSTITUTION_ID, Set.of(COURSE_A_ID), AlertStatus.OPEN))
                .thenReturn(List.of(acknowledged, pending));
        when(activityRepository.findActivitiesByCourseIdIn(Set.of(COURSE_A_ID))).thenReturn(List.of(activity));
        when(studentRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(STUDENT_A_ID)))
                .thenReturn(List.of(student));

        AlertInboxResponse pendingResult = service.getInbox(
                INSTITUTION_ID, TEACHER_USER_ID, COURSE_A_ID, PERIOD_T1_ID, AlertAttentionState.PENDING);
        AlertInboxResponse acknowledgedResult = service.getInbox(
                INSTITUTION_ID, TEACHER_USER_ID, COURSE_A_ID, PERIOD_T1_ID, AlertAttentionState.ACKNOWLEDGED);
        AlertInboxResponse allResult =
                service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_A_ID, PERIOD_T1_ID, AlertAttentionState.ALL);
        AlertInboxResponse omittedResult = service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_A_ID, PERIOD_T1_ID);

        assertEquals(
                List.of(ALERT_CRITICAL_LOW_ID),
                pendingResult.alerts().stream()
                        .map(AlertInboxResponse.AlertItem::id)
                        .toList());
        assertEquals(
                List.of(ALERT_WARNING_ID),
                acknowledgedResult.alerts().stream()
                        .map(AlertInboxResponse.AlertItem::id)
                        .toList());
        assertEquals(2, allResult.total());
        assertEquals(2, omittedResult.total());
        assertEquals(
                Instant.parse("2026-09-02T12:00:00Z"),
                acknowledgedResult.alerts().getFirst().acknowledgedAt());
    }

    private static AcademicCourse course(UUID id, String name, String subject) {
        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(id);
        when(course.getAcademicYearId()).thenReturn(ACADEMIC_YEAR_ID);
        when(course.getName()).thenReturn(name);
        when(course.getSubject()).thenReturn(subject);
        return course;
    }

    private static Activity activity(UUID id, UUID courseId, String name, String dueDate) {
        return activity(id, courseId, null, name, dueDate);
    }

    private static Activity activity(UUID id, UUID courseId, UUID academicPeriodId, String name, String dueDate) {
        Activity activity = mock(Activity.class);
        when(activity.getId()).thenReturn(id);
        when(activity.getCourseId()).thenReturn(courseId);
        when(activity.getAcademicPeriodId()).thenReturn(academicPeriodId);
        when(activity.getName()).thenReturn(name);
        when(activity.getMaxScore()).thenReturn(new BigDecimal("10.00"));
        when(activity.getDueDate()).thenReturn(LocalDate.parse(dueDate));
        return activity;
    }

    private static AcademicPeriod period(UUID id) {
        AcademicPeriod period = mock(AcademicPeriod.class);
        when(period.getId()).thenReturn(id);
        when(period.getAcademicYearId()).thenReturn(ACADEMIC_YEAR_ID);
        return period;
    }

    private static Student student(UUID id, UUID institutionId, String fullName) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        when(student.getInstitutionId()).thenReturn(institutionId);
        when(student.getFullName()).thenReturn(fullName);
        return student;
    }

    private static Alert alert(
            UUID id,
            UUID institutionId,
            UUID courseId,
            UUID activityId,
            UUID studentId,
            AlertSeverity severity,
            String score,
            boolean open) {
        Alert alert = mock(Alert.class);
        when(alert.getId()).thenReturn(id);
        when(alert.getInstitutionId()).thenReturn(institutionId);
        when(alert.getCourseId()).thenReturn(courseId);
        when(alert.getActivityId()).thenReturn(activityId);
        when(alert.getStudentId()).thenReturn(studentId);
        when(alert.getRuleCode()).thenReturn("LOW_GRADE");
        when(alert.getSeverity()).thenReturn(severity);
        when(alert.getScoreSnapshot()).thenReturn(new BigDecimal(score));
        when(alert.isOpen()).thenReturn(open);
        return alert;
    }

    private static Alert attentionAlert(UUID id, UUID activityId, AlertSeverity severity, Instant acknowledgedAt) {
        Alert alert = alert(
                id,
                INSTITUTION_ID,
                COURSE_A_ID,
                activityId,
                STUDENT_A_ID,
                severity,
                severity == AlertSeverity.CRITICAL ? "4.50" : "6.50",
                true);
        when(alert.getAcknowledgedAt()).thenReturn(acknowledgedAt);
        when(alert.isPending()).thenReturn(acknowledgedAt == null);
        when(alert.isAcknowledged()).thenReturn(acknowledgedAt != null);
        return alert;
    }
}
