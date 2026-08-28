package io.academicmonitor.demo.application;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
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
import io.academicmonitor.demo.infrastructure.platform.FakeAcademicPlatformAdapterFactory;
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
    private final FakeAcademicPlatformAdapterFactory platformAdapterFactory;

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
            AlertEvaluationService alertEvaluationService,
            FakeAcademicPlatformAdapterFactory platformAdapterFactory) {

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
        this.platformAdapterFactory = platformAdapterFactory;
    }

    @Transactional
    public DemoSyncResult sync(DemoScenario scenario) {

        User teacher = userRepository
                .findByEmail("demo.teacher@academicmonitor.local")
                .orElseGet(() -> userRepository.save(new User("demo.teacher@academicmonitor.local")));

        Institution institution = resolveInstitution(teacher);

        AcademicPlatformPort platform = platformAdapterFactory.create(scenario);

        AcademicPlatformContext context = new AcademicPlatformContext(institution.getId(), teacher.getId());

        AcademicPlatformSnapshot snapshot = platform.fetchSnapshot(context);

        PlatformCourseSnapshot platformCourse = snapshot.courses().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Academic platform returned no courses"));

        AcademicCourse course = synchronizeCourse(institution, teacher, platformCourse);

        synchronizeStudents(institution, course, platformCourse);

        int gradesProcessed = synchronizeActivitiesAndGrades(institution, course, platformCourse);

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
                platformCourse.students().size(),
                gradesProcessed,
                openAlerts.size(),
                warnings,
                critical);
    }

    private AcademicCourse synchronizeCourse(
            Institution institution, User teacher, PlatformCourseSnapshot platformCourse) {

        return courseRepository
                .findByInstitutionIdAndPlatformCodeAndExternalId(
                        institution.getId(), PLATFORM, platformCourse.externalId())
                .orElseGet(() -> {
                    AcademicCourse created = new AcademicCourse(
                            institution.getId(),
                            teacher.getId(),
                            PLATFORM,
                            platformCourse.externalId(),
                            platformCourse.name(),
                            platformCourse.subject());

                    created.enableMonitoring();

                    return courseRepository.save(created);
                });
    }

    private void synchronizeStudents(
            Institution institution, AcademicCourse course, PlatformCourseSnapshot platformCourse) {

        for (PlatformStudentSnapshot platformStudent : platformCourse.students()) {

            Student student = studentRepository
                    .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                            institution.getId(), PLATFORM, platformStudent.externalId())
                    .orElseGet(() -> studentRepository.save(new Student(
                            institution.getId(),
                            PLATFORM,
                            platformStudent.externalId(),
                            platformStudent.firstName(),
                            platformStudent.lastName())));

            if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {

                enrollmentRepository.save(new CourseEnrollment(course.getId(), student.getId()));
            }
        }
    }

    private int synchronizeActivitiesAndGrades(
            Institution institution, AcademicCourse course, PlatformCourseSnapshot platformCourse) {

        int gradesProcessed = 0;

        for (PlatformActivitySnapshot platformActivity : platformCourse.activities()) {

            Activity activity = activityRepository
                    .findByCourseIdAndPlatformCodeAndExternalId(course.getId(), PLATFORM, platformActivity.externalId())
                    .orElseGet(() -> activityRepository.save(new Activity(
                            course.getId(),
                            PLATFORM,
                            platformActivity.externalId(),
                            platformActivity.name(),
                            platformActivity.maximumScore(),
                            platformActivity.dueDate())));

            for (PlatformGradeSnapshot platformGrade : platformActivity.grades()) {

                synchronizeGrade(institution, course, activity, platformGrade);

                gradesProcessed++;
            }
        }

        return gradesProcessed;
    }

    private void synchronizeGrade(
            Institution institution, AcademicCourse course, Activity activity, PlatformGradeSnapshot platformGrade) {

        Student student = studentRepository
                .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        institution.getId(), PLATFORM, platformGrade.studentExternalId())
                .orElseThrow(() -> new IllegalStateException(
                        "Grade references unknown student: " + platformGrade.studentExternalId()));

        Grade grade = gradeRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId())
                .orElseGet(() -> new Grade(
                        activity.getId(), student.getId(), platformGrade.score(), platformGrade.recordedAt()));

        grade.changeScore(platformGrade.score());

        gradeRepository.save(grade);

        alertEvaluationService.evaluate(
                institution.getId(), course.getId(), activity.getId(), student.getId(), platformGrade.score());
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
}
