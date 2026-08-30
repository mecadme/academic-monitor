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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ActivityGradeSynchronizer {

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

        return new Result(gradesProcessed, activityIds);
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

    record Result(int gradesProcessed, List<UUID> activityIds) {

        Result {
            activityIds = activityIds == null ? List.of() : List.copyOf(activityIds);
        }
    }
}
