package io.academicmonitor.dashboard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcademicDashboardQueryServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COURSE_A_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID COURSE_B_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_A_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID STUDENT_B_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID STUDENT_SHARED_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private AcademicCourseRepository courseRepository;
    private CourseEnrollmentRepository enrollmentRepository;
    private ActivityRepository activityRepository;
    private AcademicYearRepository academicYearRepository;
    private AlertRepository alertRepository;
    private AcademicDashboardQueryService service;

    @BeforeEach
    void setUp() {
        courseRepository = mock(AcademicCourseRepository.class);
        enrollmentRepository = mock(CourseEnrollmentRepository.class);
        activityRepository = mock(ActivityRepository.class);
        academicYearRepository = mock(AcademicYearRepository.class);
        alertRepository = mock(AlertRepository.class);

        service = new AcademicDashboardQueryService(
                courseRepository, enrollmentRepository, activityRepository, academicYearRepository, alertRepository);
    }

    @Test
    void returnsEmptyDashboardWithoutIssuingChildQueriesWhenTeacherHasNoCourses() {
        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of());

        AcademicDashboardResponse result = service.getDashboard(INSTITUTION_ID, TEACHER_USER_ID);

        assertEquals(INSTITUTION_ID, result.institutionId());
        assertEquals(TEACHER_USER_ID, result.teacherUserId());
        assertEquals(new AcademicDashboardResponse.DashboardSummary(0, 0, 0, 0, 0, 0), result.summary());
        assertEquals(List.of(), result.courses());
        verifyNoInteractions(enrollmentRepository, activityRepository, academicYearRepository, alertRepository);
    }

    @Test
    void aggregatesCoursesStudentsActivitiesOpenAlertsSeveritiesAndAcademicYears() {
        AcademicCourse courseA = course(COURSE_A_ID, "1.º BGU A", "Física", ACADEMIC_YEAR_ID);
        AcademicCourse courseB = course(COURSE_B_ID, "1.º BGU B", "Física", null);

        Set<UUID> courseIds = Set.of(COURSE_A_ID, COURSE_B_ID);
        List<CourseEnrollment> enrollments = List.of(
                enrollment(COURSE_A_ID, STUDENT_A_ID),
                enrollment(COURSE_A_ID, STUDENT_SHARED_ID),
                enrollment(COURSE_B_ID, STUDENT_B_ID),
                enrollment(COURSE_B_ID, STUDENT_SHARED_ID));
        List<Activity> activities = List.of(activity(COURSE_A_ID), activity(COURSE_A_ID), activity(COURSE_B_ID));
        List<Alert> openAlerts = List.of(
                alert(COURSE_A_ID, AlertSeverity.WARNING),
                alert(COURSE_A_ID, AlertSeverity.CRITICAL),
                alert(COURSE_B_ID, AlertSeverity.WARNING));
        AcademicYear academicYear = mock(AcademicYear.class);

        when(academicYear.getId()).thenReturn(ACADEMIC_YEAR_ID);
        when(academicYear.getName()).thenReturn("2025 - 2026");
        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID))
                .thenReturn(List.of(courseB, courseA));
        when(enrollmentRepository.findEnrollmentsByCourseIdIn(courseIds)).thenReturn(enrollments);
        when(activityRepository.findActivitiesByCourseIdIn(courseIds)).thenReturn(activities);
        when(alertRepository.findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, courseIds, AlertStatus.OPEN))
                .thenReturn(openAlerts);
        when(academicYearRepository.findByInstitutionIdAndIdIn(INSTITUTION_ID, Set.of(ACADEMIC_YEAR_ID)))
                .thenReturn(List.of(academicYear));

        AcademicDashboardResponse result = service.getDashboard(INSTITUTION_ID, TEACHER_USER_ID);

        assertEquals(new AcademicDashboardResponse.DashboardSummary(2, 3, 3, 3, 2, 1), result.summary());
        assertEquals(2, result.courses().size());

        AcademicDashboardResponse.CourseSummary first = result.courses().get(0);
        assertEquals(COURSE_A_ID, first.id());
        assertEquals("1.º BGU A", first.name());
        assertEquals("Física", first.subject());
        assertEquals("2025 - 2026", first.academicYear());
        assertEquals(2, first.students());
        assertEquals(2, first.activities());
        assertEquals(2, first.openAlerts());
        assertEquals(1, first.warnings());
        assertEquals(1, first.critical());

        AcademicDashboardResponse.CourseSummary second = result.courses().get(1);
        assertEquals(COURSE_B_ID, second.id());
        assertNull(second.academicYear());
        assertEquals(2, second.students());
        assertEquals(1, second.activities());
        assertEquals(1, second.openAlerts());
        assertEquals(1, second.warnings());
        assertEquals(0, second.critical());

        verify(courseRepository).findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_USER_ID);
        verify(alertRepository).findByInstitutionIdAndCourseIdInAndStatus(INSTITUTION_ID, courseIds, AlertStatus.OPEN);
    }

    private static AcademicCourse course(UUID id, String name, String subject, UUID academicYearId) {
        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(id);
        when(course.getName()).thenReturn(name);
        when(course.getSubject()).thenReturn(subject);
        when(course.getAcademicYearId()).thenReturn(academicYearId);
        return course;
    }

    private static CourseEnrollment enrollment(UUID courseId, UUID studentId) {
        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getCourseId()).thenReturn(courseId);
        when(enrollment.getStudentId()).thenReturn(studentId);
        return enrollment;
    }

    private static Activity activity(UUID courseId) {
        Activity activity = mock(Activity.class);
        when(activity.getCourseId()).thenReturn(courseId);
        return activity;
    }

    private static Alert alert(UUID courseId, AlertSeverity severity) {
        Alert alert = mock(Alert.class);
        when(alert.getCourseId()).thenReturn(courseId);
        when(alert.getSeverity()).thenReturn(severity);
        return alert;
    }
}
