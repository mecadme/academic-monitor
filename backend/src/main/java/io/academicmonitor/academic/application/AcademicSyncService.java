package io.academicmonitor.academic.application;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
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
import io.academicmonitor.monitoring.application.AlertEvaluationService;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicSyncService {

    private final AcademicCourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final AlertRepository alertRepository;
    private final AlertEvaluationService alertEvaluationService;

    public AcademicSyncService(
            AcademicCourseRepository courseRepository,
            StudentRepository studentRepository,
            CourseEnrollmentRepository enrollmentRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            AlertRepository alertRepository,
            AlertEvaluationService alertEvaluationService) {

        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.alertRepository = alertRepository;
        this.alertEvaluationService = alertEvaluationService;
    }

    @Transactional
    public AcademicSyncResult synchronize(
            UUID institutionId, UUID teacherUserId, String platformCode, AcademicPlatformPort platform) {

        return synchronize(institutionId, teacherUserId, platformCode, platform, AcademicPlatformFilter.all());
    }

    @Transactional
    public AcademicSyncResult synchronize(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            AcademicPlatformPort platform,
            AcademicPlatformFilter filter) {

        requireId(institutionId, "institutionId");

        requireId(teacherUserId, "teacherUserId");

        requireText(platformCode, "platformCode");

        if (platform == null) {
            throw new IllegalArgumentException("platform is required");
        }

        AcademicPlatformFilter effectiveFilter = filter == null ? AcademicPlatformFilter.all() : filter;

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        AcademicPlatformSnapshot snapshot = platform.fetchSnapshot(context, effectiveFilter);

        PlatformCourseSnapshot platformCourse = resolveSingleCourse(snapshot);

        return synchronizePlatformCourse(institutionId, teacherUserId, platformCode, platformCourse);
    }

    @Transactional
    public AcademicBatchSyncResult synchronizeAll(
            UUID institutionId, UUID teacherUserId, String platformCode, AcademicPlatformPort platform) {

        return synchronizeAll(institutionId, teacherUserId, platformCode, platform, AcademicPlatformFilter.all());
    }

    @Transactional
    public AcademicBatchSyncResult synchronizeAll(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            AcademicPlatformPort platform,
            AcademicPlatformFilter filter) {

        requireId(institutionId, "institutionId");

        requireId(teacherUserId, "teacherUserId");

        requireText(platformCode, "platformCode");

        if (platform == null) {
            throw new IllegalArgumentException("platform is required");
        }

        AcademicPlatformFilter effectiveFilter = filter == null ? AcademicPlatformFilter.all() : filter;

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        AcademicPlatformSnapshot snapshot = platform.fetchSnapshot(context, effectiveFilter);

        validateSnapshot(snapshot);

        List<AcademicSyncResult> results = new ArrayList<>();

        for (PlatformCourseSnapshot platformCourse : snapshot.courses()) {

            AcademicSyncResult result =
                    synchronizePlatformCourse(institutionId, teacherUserId, platformCode, platformCourse);

            results.add(result);
        }

        return new AcademicBatchSyncResult(results);
    }

    private AcademicSyncResult synchronizePlatformCourse(
            UUID institutionId, UUID teacherUserId, String platformCode, PlatformCourseSnapshot platformCourse) {

        AcademicCourse course = synchronizeCourse(institutionId, teacherUserId, platformCode, platformCourse);

        synchronizeStudents(institutionId, platformCode, course, platformCourse);

        ActivityGradeSyncResult processingResult =
                synchronizeActivitiesAndGrades(institutionId, platformCode, course, platformCourse);

        List<Alert> openAlerts = findOpenAlertsForProcessedActivities(course.getId(), processingResult.activityIds());

        long warnings = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        long critical = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        return new AcademicSyncResult(
                course.getId(),
                course.getName(),
                platformCourse.students().size(),
                processingResult.gradesProcessed(),
                openAlerts.size(),
                warnings,
                critical);
    }

    private List<Alert> findOpenAlertsForProcessedActivities(UUID courseId, List<UUID> activityIds) {

        if (activityIds.isEmpty()) {
            return List.of();
        }

        return alertRepository.findByCourseIdAndStatusAndActivityIdIn(courseId, AlertStatus.OPEN, activityIds);
    }

    private PlatformCourseSnapshot resolveSingleCourse(AcademicPlatformSnapshot snapshot) {

        validateSnapshot(snapshot);

        return snapshot.courses().getFirst();
    }

    private static void validateSnapshot(AcademicPlatformSnapshot snapshot) {

        if (snapshot == null) {
            throw new IllegalStateException("Academic platform returned no snapshot");
        }

        if (snapshot.courses().isEmpty()) {
            throw new IllegalStateException("Academic platform returned no courses");
        }
    }

    private AcademicCourse synchronizeCourse(
            UUID institutionId, UUID teacherUserId, String platformCode, PlatformCourseSnapshot platformCourse) {

        return courseRepository
                .findByInstitutionIdAndPlatformCodeAndExternalId(
                        institutionId, platformCode, platformCourse.externalId())
                .orElseGet(() -> {
                    AcademicCourse created = new AcademicCourse(
                            institutionId,
                            teacherUserId,
                            platformCode,
                            platformCourse.externalId(),
                            platformCourse.name(),
                            platformCourse.subject());

                    created.enableMonitoring();

                    return courseRepository.save(created);
                });
    }

    private void synchronizeStudents(
            UUID institutionId, String platformCode, AcademicCourse course, PlatformCourseSnapshot platformCourse) {

        for (PlatformStudentSnapshot platformStudent : platformCourse.students()) {

            Student student = studentRepository
                    .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                            institutionId, platformCode, platformStudent.externalId())
                    .orElseGet(() -> studentRepository.save(new Student(
                            institutionId,
                            platformCode,
                            platformStudent.externalId(),
                            platformStudent.firstName(),
                            platformStudent.lastName())));

            if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {

                enrollmentRepository.save(new CourseEnrollment(course.getId(), student.getId()));
            }
        }
    }

    private ActivityGradeSyncResult synchronizeActivitiesAndGrades(
            UUID institutionId, String platformCode, AcademicCourse course, PlatformCourseSnapshot platformCourse) {

        int gradesProcessed = 0;

        List<UUID> activityIds = new ArrayList<>();

        for (PlatformActivitySnapshot platformActivity : platformCourse.activities()) {

            Activity activity = synchronizeActivity(platformCode, course, platformActivity);

            activityIds.add(activity.getId());

            for (PlatformGradeSnapshot platformGrade : platformActivity.grades()) {

                synchronizeGrade(institutionId, platformCode, course, activity, platformGrade);

                gradesProcessed++;
            }
        }

        return new ActivityGradeSyncResult(gradesProcessed, activityIds);
    }

    private Activity synchronizeActivity(
            String platformCode, AcademicCourse course, PlatformActivitySnapshot platformActivity) {

        return activityRepository
                .findByCourseIdAndPlatformCodeAndExternalId(course.getId(), platformCode, platformActivity.externalId())
                .orElseGet(() -> activityRepository.save(new Activity(
                        course.getId(),
                        platformCode,
                        platformActivity.externalId(),
                        platformActivity.name(),
                        platformActivity.maximumScore(),
                        platformActivity.dueDate())));
    }

    private void synchronizeGrade(
            UUID institutionId,
            String platformCode,
            AcademicCourse course,
            Activity activity,
            PlatformGradeSnapshot platformGrade) {

        Student student = studentRepository
                .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        institutionId, platformCode, platformGrade.studentExternalId())
                .orElseThrow(() -> new IllegalStateException(
                        "Grade references unknown student: " + platformGrade.studentExternalId()));

        Grade grade = gradeRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId())
                .orElseGet(() -> new Grade(
                        activity.getId(), student.getId(), platformGrade.score(), platformGrade.recordedAt()));

        grade.changeScore(platformGrade.score());

        gradeRepository.save(grade);

        alertEvaluationService.evaluate(
                institutionId, course.getId(), activity.getId(), student.getId(), platformGrade.score());
    }

    private static UUID requireId(UUID value, String field) {

        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }

        return value;
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }

    private record ActivityGradeSyncResult(int gradesProcessed, List<UUID> activityIds) {

        private ActivityGradeSyncResult {
            activityIds = activityIds == null ? List.of() : List.copyOf(activityIds);
        }
    }
}
