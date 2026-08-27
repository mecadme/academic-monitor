package io.academicmonitor.demo.application;

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
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.monitoring.application.AlertEvaluationService;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoSyncService {

    private static final String PLATFORM = "DEMO";

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final AcademicCourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final AlertRepository alertRepository;
    private final AlertEvaluationService alertEvaluationService;

    public DemoSyncService(
            UserRepository userRepository,
            InstitutionRepository institutionRepository,
            InstitutionMembershipRepository membershipRepository,
            AcademicCourseRepository courseRepository,
            StudentRepository studentRepository,
            CourseEnrollmentRepository enrollmentRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            AlertRepository alertRepository,
            AlertEvaluationService alertEvaluationService) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.membershipRepository = membershipRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.alertRepository = alertRepository;
        this.alertEvaluationService = alertEvaluationService;
    }

    @Transactional
    public DemoSyncResult sync(DemoScenario scenario) {

        User teacher = userRepository
                .findByEmail("demo.teacher@academicmonitor.local")
                .orElseGet(() -> userRepository.save(new User("demo.teacher@academicmonitor.local")));

        Institution institution = resolveInstitution(teacher);

        AcademicCourse course = courseRepository
                .findByInstitutionIdAndPlatformCodeAndExternalId(institution.getId(), PLATFORM, "physics-1bgu-a")
                .orElseGet(() -> {
                    AcademicCourse created = new AcademicCourse(
                            institution.getId(), teacher.getId(), PLATFORM, "physics-1bgu-a", "1.º BGU A", "Física");

                    created.enableMonitoring();

                    return courseRepository.save(created);
                });

        Activity activity = activityRepository
                .findByCourseIdAndPlatformCodeAndExternalId(course.getId(), PLATFORM, "activity-mru-001")
                .orElseGet(() -> activityRepository.save(new Activity(
                        course.getId(),
                        PLATFORM,
                        "activity-mru-001",
                        "Movimiento rectilíneo",
                        new BigDecimal("10.00"),
                        LocalDate.of(2026, 9, 25))));

        List<DemoStudent> demoStudents = List.of(
                new DemoStudent("student-001", "Ana", "Torres", "9.20", "9.20"),
                new DemoStudent("student-002", "Carlos", "Vega", "7.00", "7.00"),
                new DemoStudent("student-003", "Sofía", "López", "6.40", "8.10"),
                new DemoStudent("student-004", "Mateo", "Cárdenas", "4.80", "8.50"));

        for (DemoStudent demoStudent : demoStudents) {

            Student student = studentRepository
                    .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                            institution.getId(), PLATFORM, demoStudent.externalId())
                    .orElseGet(() -> studentRepository.save(new Student(
                            institution.getId(),
                            PLATFORM,
                            demoStudent.externalId(),
                            demoStudent.firstName(),
                            demoStudent.lastName())));

            if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {

                enrollmentRepository.save(new CourseEnrollment(course.getId(), student.getId()));
            }

            BigDecimal score = new BigDecimal(
                    scenario == DemoScenario.IMPROVED ? demoStudent.improvedScore() : demoStudent.initialScore());

            Grade grade = gradeRepository
                    .findByActivityIdAndStudentId(activity.getId(), student.getId())
                    .orElseGet(() -> new Grade(activity.getId(), student.getId(), score, Instant.now()));

            grade.changeScore(score);
            gradeRepository.save(grade);

            alertEvaluationService.evaluate(
                    institution.getId(), course.getId(), activity.getId(), student.getId(), score);
        }

        List<Alert> openAlerts = alertRepository.findByCourseIdAndStatus(course.getId(), AlertStatus.OPEN);

        long warnings = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        long critical = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        return new DemoSyncResult(
                institution.getId(),
                teacher.getId(),
                course.getId(),
                course.getName(),
                scenario,
                demoStudents.size(),
                demoStudents.size(),
                openAlerts.size(),
                warnings,
                critical);
    }

    private Institution resolveInstitution(User teacher) {

        return membershipRepository.findByUserId(teacher.getId()).stream()
                .filter(InstitutionMembership::isActive)
                .findFirst()
                .flatMap(membership -> institutionRepository.findById(membership.getInstitutionId()))
                .orElseGet(() -> {
                    Institution institution =
                            institutionRepository.save(new Institution("Unidad Educativa Demo", "America/Guayaquil"));

                    membershipRepository.save(
                            new InstitutionMembership(teacher.getId(), institution.getId(), InstitutionRole.TEACHER));

                    return institution;
                });
    }

    private record DemoStudent(
            String externalId, String firstName, String lastName, String initialScore, String improvedScore) {}
}
