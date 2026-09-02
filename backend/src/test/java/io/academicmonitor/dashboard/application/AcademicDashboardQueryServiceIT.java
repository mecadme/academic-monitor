package io.academicmonitor.dashboard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcademicDashboardQueryServiceIT extends PostgresIntegrationTest {

    @Autowired
    private AcademicDashboardQueryService service;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicCourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void aggregatesPersistedDataAndExcludesOtherInstitutionsTeachersAndResolvedAlerts() {
        Institution institution = institutionRepository.save(new Institution("Dashboard School", "America/Guayaquil"));
        Institution otherInstitution =
                institutionRepository.save(new Institution("Other Dashboard School", "America/Guayaquil"));
        User teacher = userRepository.save(new User("dashboard.teacher@example.com"));
        User otherTeacher = userRepository.save(new User("other.dashboard.teacher@example.com"));

        AcademicYear academicYear = academicYearRepository.save(new AcademicYear(
                institution.getId(), "TEST", "year-2025", "2025 - 2026", "2025-2026", new BigDecimal("10.00")));
        AcademicYear otherAcademicYear = academicYearRepository.save(new AcademicYear(
                otherInstitution.getId(), "TEST", "year-other", "Other year", "2025-2026", new BigDecimal("10.00")));

        AcademicCourse courseA = saveCourse(institution, teacher, academicYear, "course-a", "1.º BGU A", "Física");
        AcademicCourse courseB = saveCourse(institution, teacher, academicYear, "course-b", "1.º BGU B", "Física");
        AcademicCourse otherInstitutionCourse = saveCourse(
                otherInstitution,
                teacher,
                otherAcademicYear,
                "course-other-institution",
                "Hidden institution course",
                "Química");
        AcademicCourse otherTeacherCourse = saveCourse(
                institution, otherTeacher, academicYear, "course-other-teacher", "Hidden teacher course", "Química");

        Student sharedStudent = saveStudent(institution, "student-shared", "Ana", "Torres");
        Student studentA = saveStudent(institution, "student-a", "Carlos", "Vega");
        Student studentB = saveStudent(institution, "student-b", "Mateo", "Cárdenas");
        Student hiddenStudent = saveStudent(otherInstitution, "student-hidden", "Hidden", "Student");

        enrollmentRepository.save(new CourseEnrollment(courseA.getId(), sharedStudent.getId()));
        enrollmentRepository.save(new CourseEnrollment(courseA.getId(), studentA.getId()));
        enrollmentRepository.save(new CourseEnrollment(courseB.getId(), sharedStudent.getId()));
        enrollmentRepository.save(new CourseEnrollment(courseB.getId(), studentB.getId()));
        enrollmentRepository.save(new CourseEnrollment(otherInstitutionCourse.getId(), hiddenStudent.getId()));

        Activity courseAFirst = saveActivity(courseA, "activity-a-1", "Movimiento rectilíneo");
        saveActivity(courseA, "activity-a-2", "Caída libre");
        Activity courseBActivity = saveActivity(courseB, "activity-b-1", "Vectores");
        Activity hiddenActivity = saveActivity(otherInstitutionCourse, "activity-hidden", "Hidden activity");
        saveActivity(otherTeacherCourse, "activity-hidden-teacher", "Hidden teacher activity");

        alertRepository.save(alert(institution, courseA, courseAFirst, sharedStudent, AlertSeverity.WARNING, "6.50"));
        alertRepository.save(alert(institution, courseA, courseAFirst, studentA, AlertSeverity.CRITICAL, "4.50"));
        alertRepository.save(
                alert(institution, courseB, courseBActivity, sharedStudent, AlertSeverity.WARNING, "6.00"));

        Alert resolved = alert(institution, courseB, courseBActivity, studentB, AlertSeverity.CRITICAL, "4.00");
        resolved.resolve();
        alertRepository.save(resolved);

        alertRepository.save(alert(
                otherInstitution,
                otherInstitutionCourse,
                hiddenActivity,
                hiddenStudent,
                AlertSeverity.CRITICAL,
                "3.00"));

        entityManager.flush();
        entityManager.clear();

        AcademicDashboardResponse result = service.getDashboard(institution.getId(), teacher.getId());

        assertEquals(new AcademicDashboardResponse.DashboardSummary(2, 3, 3, 3, 2, 1), result.summary());
        assertEquals(2, result.courses().size());
        assertEquals("1.º BGU A", result.courses().get(0).name());
        assertEquals("2025 - 2026", result.courses().get(0).academicYear());
        assertEquals(2, result.courses().get(0).students());
        assertEquals(2, result.courses().get(0).activities());
        assertEquals(2, result.courses().get(0).openAlerts());
        assertEquals(1, result.courses().get(0).warnings());
        assertEquals(1, result.courses().get(0).critical());
        assertEquals("1.º BGU B", result.courses().get(1).name());
        assertEquals(2, result.courses().get(1).students());
        assertEquals(1, result.courses().get(1).activities());
        assertEquals(1, result.courses().get(1).openAlerts());
        assertEquals(1, result.courses().get(1).warnings());
        assertEquals(0, result.courses().get(1).critical());
    }

    private AcademicCourse saveCourse(
            Institution institution,
            User teacher,
            AcademicYear academicYear,
            String externalId,
            String name,
            String subject) {
        return courseRepository.save(new AcademicCourse(
                institution.getId(), teacher.getId(), academicYear.getId(), "TEST", externalId, name, subject));
    }

    private Student saveStudent(Institution institution, String externalId, String firstName, String lastName) {
        return studentRepository.save(new Student(institution.getId(), "TEST", externalId, firstName, lastName));
    }

    private Activity saveActivity(AcademicCourse course, String externalId, String name) {
        return activityRepository.save(new Activity(
                course.getId(), null, "TEST", externalId, name, new BigDecimal("10.00"), LocalDate.of(2026, 1, 15)));
    }

    private static Alert alert(
            Institution institution,
            AcademicCourse course,
            Activity activity,
            Student student,
            AlertSeverity severity,
            String score) {
        return new Alert(
                institution.getId(),
                course.getId(),
                activity.getId(),
                student.getId(),
                "LOW_GRADE",
                severity,
                new BigDecimal(score));
    }
}
