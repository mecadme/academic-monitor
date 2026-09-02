package io.academicmonitor.monitoring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
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

class AlertInboxQueryServiceIT extends PostgresIntegrationTest {

    @Autowired
    private AlertInboxQueryService service;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicCourseRepository courseRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsScopedOpenAlertsWithCourseActivityAndStudentMetadata() {
        Institution institution = institutionRepository.save(new Institution("Alert School", "America/Guayaquil"));
        Institution otherInstitution =
                institutionRepository.save(new Institution("Other Alert School", "America/Guayaquil"));
        User teacher = userRepository.save(new User("alert.teacher@example.com"));
        User otherTeacher = userRepository.save(new User("other.alert.teacher@example.com"));

        AcademicYear academicYear = academicYearRepository.save(new AcademicYear(
                institution.getId(), "TEST", "alert-year", "2025 - 2026", "2025-2026", new BigDecimal("10.00")));
        AcademicYear otherAcademicYear = academicYearRepository.save(new AcademicYear(
                otherInstitution.getId(),
                "TEST",
                "other-alert-year",
                "2025 - 2026",
                "2025-2026",
                new BigDecimal("10.00")));
        AcademicYear otherTeacherAcademicYear = academicYearRepository.save(new AcademicYear(
                institution.getId(),
                "TEST",
                "other-teacher-alert-year",
                "Other teacher year",
                "2024-2025",
                new BigDecimal("10.00")));
        AcademicPeriod periodT1 = academicPeriodRepository.save(
                new AcademicPeriod(academicYear.getId(), "period-t1", "Primer trimestre", "T1", 1));
        AcademicPeriod periodT2 = academicPeriodRepository.save(
                new AcademicPeriod(academicYear.getId(), "period-t2", "Segundo trimestre", "T2", 2));
        AcademicPeriod foreignPeriod = academicPeriodRepository.save(
                new AcademicPeriod(otherAcademicYear.getId(), "foreign-period", "Foreign period", "FP", 1));
        AcademicPeriod otherTeacherPeriod = academicPeriodRepository.save(new AcademicPeriod(
                otherTeacherAcademicYear.getId(), "other-teacher-period", "Other teacher period", "OTP", 1));

        AcademicCourse physics = saveCourse(
                institution,
                teacher,
                academicYear,
                "physics-course",
                "Primer Curso A, Bachillerato General Unificado",
                "Física");
        AcademicCourse chemistry = saveCourse(
                institution,
                teacher,
                academicYear,
                "chemistry-course",
                "Segundo Curso B, Bachillerato General Unificado",
                "Química");
        AcademicCourse otherInstitutionCourse = saveCourse(
                otherInstitution,
                teacher,
                otherAcademicYear,
                "other-institution-course",
                "Hidden institution course",
                "Matemática");
        AcademicCourse otherTeacherCourse = saveCourse(
                institution,
                otherTeacher,
                otherTeacherAcademicYear,
                "other-teacher-course",
                "Hidden teacher course",
                "Biología");

        Student ana = saveStudent(institution, "student-ana", "Ana", "Torres");
        Student bruno = saveStudent(institution, "student-bruno", "Bruno", "Vega");
        Student hiddenInstitutionStudent =
                saveStudent(otherInstitution, "student-hidden-institution", "Hidden", "Institution");
        Student hiddenTeacherStudent = saveStudent(institution, "student-hidden-teacher", "Hidden", "Teacher");

        Activity physicsActivity = saveActivity(physics, periodT1, "physics-activity", "Movimiento rectilíneo");
        Activity chemistryActivity = saveActivity(chemistry, periodT2, "chemistry-activity", "Enlaces químicos");
        Activity otherInstitutionActivity = saveActivity(
                otherInstitutionCourse, foreignPeriod, "hidden-institution-activity", "Hidden institution activity");
        Activity otherTeacherActivity = saveActivity(
                otherTeacherCourse, otherTeacherPeriod, "hidden-teacher-activity", "Hidden teacher activity");

        alertRepository.save(alert(institution, chemistry, chemistryActivity, bruno, AlertSeverity.CRITICAL, "4.50"));
        alertRepository.save(alert(institution, physics, physicsActivity, ana, AlertSeverity.WARNING, "6.50"));

        Alert resolved = alert(institution, physics, physicsActivity, bruno, AlertSeverity.CRITICAL, "3.00");
        resolved.resolve();
        alertRepository.save(resolved);

        alertRepository.save(alert(
                otherInstitution,
                otherInstitutionCourse,
                otherInstitutionActivity,
                hiddenInstitutionStudent,
                AlertSeverity.CRITICAL,
                "1.00"));
        alertRepository.save(alert(
                institution,
                otherTeacherCourse,
                otherTeacherActivity,
                hiddenTeacherStudent,
                AlertSeverity.CRITICAL,
                "2.00"));

        entityManager.flush();
        entityManager.clear();

        AlertInboxResponse result = service.getInbox(institution.getId(), teacher.getId(), null);

        assertEquals(institution.getId(), result.institutionId());
        assertEquals(teacher.getId(), result.teacherUserId());
        assertEquals(2, result.total());

        AlertInboxResponse.AlertItem critical = result.alerts().getFirst();
        assertEquals(AlertSeverity.CRITICAL, critical.severity());
        assertEquals(new BigDecimal("4.50"), critical.score());
        assertEquals("LOW_GRADE", critical.ruleCode());
        assertEquals(chemistry.getId(), critical.course().id());
        assertEquals(
                "Segundo Curso B, Bachillerato General Unificado",
                critical.course().name());
        assertEquals("Química", critical.course().subject());
        assertEquals(chemistryActivity.getId(), critical.activity().id());
        assertEquals("Enlaces químicos", critical.activity().name());
        assertEquals(new BigDecimal("10.00"), critical.activity().maximumScore());
        assertEquals(LocalDate.of(2026, 1, 15), critical.activity().dueDate());
        assertEquals(bruno.getId(), critical.student().id());
        assertEquals("Bruno Vega", critical.student().name());

        AlertInboxResponse.AlertItem warning = result.alerts().get(1);
        assertEquals(AlertSeverity.WARNING, warning.severity());
        assertEquals(physics.getId(), warning.course().id());
        assertEquals(ana.getId(), warning.student().id());

        AlertInboxResponse foreignFilter =
                service.getInbox(institution.getId(), teacher.getId(), otherTeacherCourse.getId());
        assertEquals(0, foreignFilter.total());
        assertEquals(0, foreignFilter.alerts().size());

        AlertInboxResponse t1 = service.getInbox(institution.getId(), teacher.getId(), null, periodT1.getId());
        assertEquals(1, t1.total());
        assertEquals(physics.getId(), t1.alerts().getFirst().course().id());

        AlertInboxResponse t2 = service.getInbox(institution.getId(), teacher.getId(), null, periodT2.getId());
        assertEquals(1, t2.total());
        assertEquals(chemistry.getId(), t2.alerts().getFirst().course().id());

        AlertInboxResponse composed =
                service.getInbox(institution.getId(), teacher.getId(), physics.getId(), periodT2.getId());
        assertEquals(0, composed.total());

        AlertInboxResponse foreignPeriodFilter =
                service.getInbox(institution.getId(), teacher.getId(), null, foreignPeriod.getId());
        assertEquals(0, foreignPeriodFilter.total());

        AlertInboxResponse otherTeacherPeriodFilter =
                service.getInbox(institution.getId(), teacher.getId(), null, otherTeacherPeriod.getId());
        assertEquals(0, otherTeacherPeriodFilter.total());
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

    private Activity saveActivity(AcademicCourse course, AcademicPeriod period, String externalId, String name) {
        return activityRepository.save(new Activity(
                course.getId(),
                period.getId(),
                "TEST",
                externalId,
                name,
                new BigDecimal("10.00"),
                LocalDate.of(2026, 1, 15)));
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
