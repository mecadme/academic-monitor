package io.academicmonitor.academic.application;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActivityGradeSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityGradeSynchronizer.class);

    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final AlertEvaluationService alertEvaluationService;

    public ActivityGradeSynchronizer(
            StudentRepository studentRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            AlertEvaluationService alertEvaluationService) {

        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.alertEvaluationService = alertEvaluationService;
    }

    Result synchronize(
            UUID institutionId,
            String platformCode,
            AcademicCourse course,
            PlatformCourseSnapshot platformCourse,
            Map<String, UUID> periodIdsByExternalId) {

        int gradesProcessed = 0;

        List<UUID> activityIds = new ArrayList<>();
        Map<GradeIdentity, ResolvedGrade> gradesByIdentity = new LinkedHashMap<>();

        for (PlatformActivitySnapshot platformActivity : platformCourse.activities()) {

            UUID academicPeriodId = resolveAcademicPeriodId(platformActivity, periodIdsByExternalId);

            Activity activity = synchronizeActivity(platformCode, course, platformActivity, academicPeriodId);

            activityIds.add(activity.getId());

            for (PlatformGradeSnapshot platformGrade : platformActivity.grades()) {
                Student student = resolveStudent(institutionId, platformCode, platformGrade);
                GradeIdentity identity = new GradeIdentity(activity.getId(), student.getId());
                ResolvedGrade candidate = new ResolvedGrade(activity, student, platformGrade);
                ResolvedGrade existing = gradesByIdentity.putIfAbsent(identity, candidate);

                if (existing == null) {
                    continue;
                }

                LOGGER.warn(
                        "Platform snapshot contains duplicate grade for activity {} and student {}; consolidating it by natural key",
                        activity.getId(),
                        student.getId());

                if (isMoreRecent(candidate.platformGrade(), existing.platformGrade())) {
                    gradesByIdentity.put(identity, candidate);
                }
            }
        }

        for (ResolvedGrade grade : gradesByIdentity.values()) {
            synchronizeGrade(institutionId, course, grade);
            gradesProcessed++;
        }

        return new Result(gradesProcessed, activityIds);
    }

    private Activity synchronizeActivity(
            String platformCode,
            AcademicCourse course,
            PlatformActivitySnapshot platformActivity,
            UUID academicPeriodId) {

        return activityRepository
                .findByCourseIdAndPlatformCodeAndExternalId(course.getId(), platformCode, platformActivity.externalId())
                .map(existing -> associateAcademicPeriod(existing, academicPeriodId))
                .orElseGet(() -> activityRepository.save(new Activity(
                        course.getId(),
                        academicPeriodId,
                        platformCode,
                        platformActivity.externalId(),
                        platformActivity.name(),
                        platformActivity.maximumScore(),
                        platformActivity.dueDate())));
    }

    private Activity associateAcademicPeriod(Activity activity, UUID academicPeriodId) {
        if (activity.associateAcademicPeriod(academicPeriodId)) {
            return activityRepository.save(activity);
        }
        return activity;
    }

    private static UUID resolveAcademicPeriodId(
            PlatformActivitySnapshot platformActivity, Map<String, UUID> periodIdsByExternalId) {
        String periodExternalId = platformActivity.periodExternalId();

        if (periodExternalId == null || periodExternalId.isBlank()) {
            return null;
        }

        UUID academicPeriodId = periodIdsByExternalId.get(periodExternalId);
        if (academicPeriodId == null) {
            throw new IllegalStateException("Activity " + platformActivity.externalId()
                    + " references an unknown academic period: " + periodExternalId);
        }

        return academicPeriodId;
    }

    private void synchronizeGrade(UUID institutionId, AcademicCourse course, ResolvedGrade resolvedGrade) {

        Activity activity = resolvedGrade.activity();
        Student student = resolvedGrade.student();
        PlatformGradeSnapshot platformGrade = resolvedGrade.platformGrade();

        Grade grade = gradeRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId())
                .orElseGet(() -> new Grade(
                        activity.getId(), student.getId(), platformGrade.score(), platformGrade.recordedAt()));

        grade.update(platformGrade.score(), platformGrade.recordedAt());

        gradeRepository.save(grade);

        if (course.getTeacherUserId() == null) {
            alertEvaluationService.evaluate(
                    institutionId, course.getId(), activity.getId(), student.getId(), platformGrade.score());
            return;
        }
        alertEvaluationService.evaluate(
                institutionId,
                course.getTeacherUserId(),
                course.getId(),
                activity.getId(),
                student.getId(),
                platformGrade.score(),
                activity.getAcademicPeriodId(),
                student.getFullName(),
                course.getSubject() == null ? course.getName() : course.getSubject());
    }

    private Student resolveStudent(UUID institutionId, String platformCode, PlatformGradeSnapshot platformGrade) {
        return studentRepository
                .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        institutionId, platformCode, platformGrade.studentExternalId())
                .orElseThrow(() -> new IllegalStateException(
                        "Grade references unknown student: " + platformGrade.studentExternalId()));
    }

    private static boolean isMoreRecent(PlatformGradeSnapshot candidate, PlatformGradeSnapshot current) {
        if (candidate.recordedAt() == null) {
            return false;
        }
        return current.recordedAt() == null || candidate.recordedAt().isAfter(current.recordedAt());
    }

    private record GradeIdentity(UUID activityId, UUID studentId) {}

    private record ResolvedGrade(Activity activity, Student student, PlatformGradeSnapshot platformGrade) {}

    record Result(int gradesProcessed, List<UUID> activityIds) {

        Result {
            activityIds = activityIds == null ? List.of() : List.copyOf(activityIds);
        }
    }
}
