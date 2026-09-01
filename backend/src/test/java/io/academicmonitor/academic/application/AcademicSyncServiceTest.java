package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformGradeSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Grade;
import io.academicmonitor.academic.domain.GradeRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.monitoring.application.AlertEvaluationService;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicSyncServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID STUDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID ACTIVITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID ACADEMIC_PERIOD_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final String PLATFORM = "TEST";

    @Mock
    private AcademicCourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    private AcademicSyncService service;

    private AcademicCalendarSynchronizer academicCalendarSynchronizer;

    @BeforeEach
    void setUp() {

        academicCalendarSynchronizer = mock(AcademicCalendarSynchronizer.class);

        lenient()
                .when(academicCalendarSynchronizer.synchronize(
                        org.mockito.ArgumentMatchers.eq(INSTITUTION_ID),
                        org.mockito.ArgumentMatchers.eq(PLATFORM),
                        any(PlatformAcademicYearSnapshot.class)))
                .thenReturn(new AcademicCalendarSynchronizer.Result(
                        ACADEMIC_YEAR_ID, Map.of("period-001", ACADEMIC_PERIOD_ID)));

        CourseRosterSynchronizer courseRosterSynchronizer =
                new CourseRosterSynchronizer(courseRepository, studentRepository, enrollmentRepository);

        ActivityGradeSynchronizer activityGradeSynchronizer = new ActivityGradeSynchronizer(
                studentRepository, activityRepository, gradeRepository, alertEvaluationService);

        SyncAlertSummaryService alertSummaryService = new SyncAlertSummaryService(alertRepository);

        service = new AcademicSyncService(
                academicCalendarSynchronizer, courseRosterSynchronizer, activityGradeSynchronizer, alertSummaryService);
    }

    @Test
    void synchronizeCreatesCourseStudentEnrollmentActivityAndGrade() {

        AcademicCourse persistedCourse = org.mockito.Mockito.mock(AcademicCourse.class);

        Student persistedStudent = org.mockito.Mockito.mock(Student.class);

        Activity persistedActivity = org.mockito.Mockito.mock(Activity.class);

        when(persistedCourse.getId()).thenReturn(COURSE_ID);

        when(persistedCourse.getName()).thenReturn("1.º BGU A");

        when(persistedStudent.getId()).thenReturn(STUDENT_ID);

        when(persistedActivity.getId()).thenReturn(ACTIVITY_ID);

        when(courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "physics-1bgu-a"))
                .thenReturn(Optional.empty());

        when(courseRepository.save(any(AcademicCourse.class))).thenReturn(persistedCourse);

        when(studentRepository.findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "student-001"))
                .thenReturn(Optional.empty(), Optional.of(persistedStudent));

        when(studentRepository.save(any(Student.class))).thenReturn(persistedStudent);

        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID))
                .thenReturn(false);

        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-mru-001"))
                .thenReturn(Optional.empty());

        when(activityRepository.save(any(Activity.class))).thenReturn(persistedActivity);

        when(gradeRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        when(alertRepository.findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID)))
                .thenReturn(List.of());

        AcademicSyncResult result =
                service.synchronize(INSTITUTION_ID, TEACHER_ID, PLATFORM, platformWithScore("4.80"));

        assertEquals(COURSE_ID, result.courseId());

        assertEquals("1.º BGU A", result.courseName());

        assertEquals(1, result.students());

        assertEquals(1, result.gradesProcessed());

        assertEquals(0, result.openAlerts());

        assertEquals(0, result.warnings());

        assertEquals(0, result.critical());

        ArgumentCaptor<AcademicCourse> courseCaptor = ArgumentCaptor.forClass(AcademicCourse.class);

        verify(courseRepository).save(courseCaptor.capture());

        AcademicCourse createdCourse = courseCaptor.getValue();

        assertEquals(INSTITUTION_ID, createdCourse.getInstitutionId());

        assertEquals(TEACHER_ID, createdCourse.getTeacherUserId());

        assertEquals(ACADEMIC_YEAR_ID, createdCourse.getAcademicYearId());

        assertEquals(PLATFORM, createdCourse.getPlatformCode());

        assertEquals("physics-1bgu-a", createdCourse.getExternalId());

        assertEquals("1.º BGU A", createdCourse.getName());

        assertEquals("Física", createdCourse.getSubject());

        assertTrue(createdCourse.isMonitoringEnabled());

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        verify(studentRepository).save(studentCaptor.capture());

        Student createdStudent = studentCaptor.getValue();

        assertEquals(INSTITUTION_ID, createdStudent.getInstitutionId());

        assertEquals("student-001", createdStudent.getExternalId());

        assertEquals("Ana", createdStudent.getFirstName());

        assertEquals("Torres", createdStudent.getLastName());

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);

        verify(activityRepository).save(activityCaptor.capture());

        Activity createdActivity = activityCaptor.getValue();

        assertEquals(COURSE_ID, createdActivity.getCourseId());

        assertEquals(ACADEMIC_PERIOD_ID, createdActivity.getAcademicPeriodId());

        assertEquals("Movimiento rectilíneo", createdActivity.getName());

        assertEquals(new BigDecimal("10.00"), createdActivity.getMaxScore());

        assertEquals(LocalDate.of(2026, 9, 25), createdActivity.getDueDate());

        verify(enrollmentRepository).save(any(CourseEnrollment.class));

        ArgumentCaptor<Grade> gradeCaptor = ArgumentCaptor.forClass(Grade.class);

        verify(gradeRepository).save(gradeCaptor.capture());

        Grade createdGrade = gradeCaptor.getValue();

        assertEquals(ACTIVITY_ID, createdGrade.getActivityId());

        assertEquals(STUDENT_ID, createdGrade.getStudentId());

        assertEquals(new BigDecimal("4.80"), createdGrade.getScore());

        verify(alertEvaluationService)
                .evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("4.80"));

        verify(alertRepository)
                .findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID));
    }

    @Test
    void synchronizeDoesNotDuplicateExistingEntitiesAndUpdatesGrade() {

        AcademicCourse course = org.mockito.Mockito.mock(AcademicCourse.class);

        Student student = org.mockito.Mockito.mock(Student.class);

        Activity activity = org.mockito.Mockito.mock(Activity.class);

        when(course.getId()).thenReturn(COURSE_ID);

        when(course.getName()).thenReturn("1.º BGU A");

        when(student.getId()).thenReturn(STUDENT_ID);

        when(activity.getId()).thenReturn(ACTIVITY_ID);

        Grade existingGrade =
                new Grade(ACTIVITY_ID, STUDENT_ID, new BigDecimal("6.40"), Instant.parse("2026-08-01T12:00:00Z"));

        when(courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "physics-1bgu-a"))
                .thenReturn(Optional.of(course));

        when(studentRepository.findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "student-001"))
                .thenReturn(Optional.of(student));

        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID))
                .thenReturn(true);

        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-mru-001"))
                .thenReturn(Optional.of(activity));

        when(gradeRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingGrade));

        when(alertRepository.findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID)))
                .thenReturn(List.of());

        AcademicSyncResult result =
                service.synchronize(INSTITUTION_ID, TEACHER_ID, PLATFORM, platformWithScore("8.10"));

        assertEquals(new BigDecimal("8.10"), existingGrade.getScore());

        assertEquals(1, result.students());

        assertEquals(1, result.gradesProcessed());

        verify(courseRepository, never()).save(any(AcademicCourse.class));

        verify(studentRepository, never()).save(any(Student.class));

        verify(enrollmentRepository, never()).save(any(CourseEnrollment.class));

        verify(activityRepository, never()).save(any(Activity.class));

        verify(gradeRepository).save(existingGrade);

        verify(alertEvaluationService)
                .evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("8.10"));

        verify(alertRepository)
                .findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID));
    }

    @Test
    void synchronizeReturnsAlertSummaryForProcessedActivities() {

        AcademicCourse course = org.mockito.Mockito.mock(AcademicCourse.class);

        Student student = org.mockito.Mockito.mock(Student.class);

        Activity activity = org.mockito.Mockito.mock(Activity.class);

        when(course.getId()).thenReturn(COURSE_ID);

        when(course.getName()).thenReturn("1.º BGU A");

        when(student.getId()).thenReturn(STUDENT_ID);

        when(activity.getId()).thenReturn(ACTIVITY_ID);

        Grade existingGrade =
                new Grade(ACTIVITY_ID, STUDENT_ID, new BigDecimal("4.80"), Instant.parse("2026-08-01T12:00:00Z"));

        when(courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "physics-1bgu-a"))
                .thenReturn(Optional.of(course));

        when(studentRepository.findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        INSTITUTION_ID, PLATFORM, "student-001"))
                .thenReturn(Optional.of(student));

        when(enrollmentRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID))
                .thenReturn(true);

        when(activityRepository.findByCourseIdAndPlatformCodeAndExternalId(COURSE_ID, PLATFORM, "activity-mru-001"))
                .thenReturn(Optional.of(activity));

        when(gradeRepository.findByActivityIdAndStudentId(ACTIVITY_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingGrade));

        Alert warningOne = org.mockito.Mockito.mock(Alert.class);

        Alert warningTwo = org.mockito.Mockito.mock(Alert.class);

        Alert critical = org.mockito.Mockito.mock(Alert.class);

        when(warningOne.getSeverity()).thenReturn(AlertSeverity.WARNING);

        when(warningTwo.getSeverity()).thenReturn(AlertSeverity.WARNING);

        when(critical.getSeverity()).thenReturn(AlertSeverity.CRITICAL);

        when(alertRepository.findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID)))
                .thenReturn(List.of(warningOne, warningTwo, critical));

        AcademicSyncResult result =
                service.synchronize(INSTITUTION_ID, TEACHER_ID, PLATFORM, platformWithScore("4.80"));

        assertEquals(3, result.openAlerts());

        assertEquals(2, result.warnings());

        assertEquals(1, result.critical());

        verify(alertRepository)
                .findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID));

        verify(alertRepository, never()).findByCourseIdAndStatus(COURSE_ID, AlertStatus.OPEN);
    }

    @Test
    void synchronizeFailsWhenPlatformReturnsNoCourses() {

        AcademicPlatformPort emptyPlatform = context -> new AcademicPlatformSnapshot(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.synchronize(INSTITUTION_ID, TEACHER_ID, PLATFORM, emptyPlatform));

        assertEquals("Academic platform returned no courses", exception.getMessage());

        verifyNoInteractions(
                academicCalendarSynchronizer,
                courseRepository,
                studentRepository,
                enrollmentRepository,
                activityRepository,
                gradeRepository,
                alertRepository,
                alertEvaluationService);
    }

    @Test
    void synchronizeAllPreservesFilterAndAggregatesEveryCourseResult() {

        CourseRosterSynchronizer courseRosterSynchronizer = mock(CourseRosterSynchronizer.class);

        ActivityGradeSynchronizer activityGradeSynchronizer = mock(ActivityGradeSynchronizer.class);

        SyncAlertSummaryService alertSummaryService = mock(SyncAlertSummaryService.class);

        AcademicCalendarSynchronizer calendarSynchronizer = mock(AcademicCalendarSynchronizer.class);

        AcademicSyncService orchestrationService = new AcademicSyncService(
                calendarSynchronizer, courseRosterSynchronizer, activityGradeSynchronizer, alertSummaryService);

        PlatformAcademicYearSnapshot academicYear = academicYear();

        PlatformCourseSnapshot firstPlatformCourse =
                new PlatformCourseSnapshot("course-001", "Course 1", "Physics", academicYear, List.of(), List.of());

        PlatformCourseSnapshot secondPlatformCourse =
                new PlatformCourseSnapshot("course-002", "Course 2", "Chemistry", academicYear, List.of(), List.of());

        AcademicPlatformSnapshot snapshot =
                new AcademicPlatformSnapshot(List.of(firstPlatformCourse, secondPlatformCourse));

        AcademicPlatformPort platform = mock(AcademicPlatformPort.class);

        AcademicPlatformFilter filter = new AcademicPlatformFilter("period-t1");

        when(platform.fetchSnapshot(new AcademicPlatformContext(INSTITUTION_ID, TEACHER_ID), filter))
                .thenReturn(snapshot);

        AcademicCalendarSynchronizer.Result calendarResult =
                new AcademicCalendarSynchronizer.Result(ACADEMIC_YEAR_ID, Map.of("period-001", ACADEMIC_PERIOD_ID));

        when(calendarSynchronizer.synchronize(INSTITUTION_ID, PLATFORM, academicYear))
                .thenReturn(calendarResult);

        AcademicCourse firstCourse = mock(AcademicCourse.class);
        AcademicCourse secondCourse = mock(AcademicCourse.class);

        UUID secondCourseId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID secondActivityId = UUID.fromString("77777777-7777-7777-7777-777777777777");

        when(firstCourse.getId()).thenReturn(COURSE_ID);
        when(firstCourse.getName()).thenReturn("Course 1");
        when(secondCourse.getId()).thenReturn(secondCourseId);
        when(secondCourse.getName()).thenReturn("Course 2");

        when(courseRosterSynchronizer.synchronize(
                        INSTITUTION_ID, TEACHER_ID, PLATFORM, firstPlatformCourse, ACADEMIC_YEAR_ID))
                .thenReturn(firstCourse);

        when(courseRosterSynchronizer.synchronize(
                        INSTITUTION_ID, TEACHER_ID, PLATFORM, secondPlatformCourse, ACADEMIC_YEAR_ID))
                .thenReturn(secondCourse);

        when(activityGradeSynchronizer.synchronize(
                        INSTITUTION_ID,
                        PLATFORM,
                        firstCourse,
                        firstPlatformCourse,
                        calendarResult.periodIdsByExternalId()))
                .thenReturn(new ActivityGradeSynchronizer.Result(10, List.of(ACTIVITY_ID)));

        when(activityGradeSynchronizer.synchronize(
                        INSTITUTION_ID,
                        PLATFORM,
                        secondCourse,
                        secondPlatformCourse,
                        calendarResult.periodIdsByExternalId()))
                .thenReturn(new ActivityGradeSynchronizer.Result(20, List.of(secondActivityId)));

        when(alertSummaryService.summarize(COURSE_ID, List.of(ACTIVITY_ID)))
                .thenReturn(new SyncAlertSummaryService.Summary(3, 2, 1));

        when(alertSummaryService.summarize(secondCourseId, List.of(secondActivityId)))
                .thenReturn(new SyncAlertSummaryService.Summary(4, 2, 2));

        AcademicBatchSyncResult result =
                orchestrationService.synchronizeAll(INSTITUTION_ID, TEACHER_ID, PLATFORM, platform, filter);

        assertEquals(2, result.coursesProcessed());
        assertEquals(30, result.gradesProcessed());
        assertEquals(7, result.openAlerts());
        assertEquals(4, result.warnings());
        assertEquals(3, result.critical());

        verify(platform).fetchSnapshot(new AcademicPlatformContext(INSTITUTION_ID, TEACHER_ID), filter);
    }

    private AcademicPlatformPort platformWithScore(String score) {

        PlatformStudentSnapshot student = new PlatformStudentSnapshot("student-001", "Ana", "Torres");

        PlatformGradeSnapshot grade =
                new PlatformGradeSnapshot("student-001", new BigDecimal(score), Instant.parse("2026-08-27T12:00:00Z"));

        PlatformActivitySnapshot activity = new PlatformActivitySnapshot(
                "activity-mru-001",
                "Movimiento rectilíneo",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 9, 25),
                "period-001",
                List.of(grade));

        PlatformCourseSnapshot course = new PlatformCourseSnapshot(
                "physics-1bgu-a", "1.º BGU A", "Física", academicYear(), List.of(activity), List.of(student));

        AcademicPlatformSnapshot snapshot = new AcademicPlatformSnapshot(List.of(course));

        return context -> snapshot;
    }

    private static PlatformAcademicYearSnapshot academicYear() {
        PlatformAcademicPeriodSnapshot period =
                new PlatformAcademicPeriodSnapshot("period-001", "First period", "P1", 1);

        return new PlatformAcademicYearSnapshot(
                "academic-year-001", "Academic year", "2026-2027", new BigDecimal("10.00"), List.of(period));
    }
}
