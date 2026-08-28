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
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDashboardService {

    private final AcademicCourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final AlertRepository alertRepository;

    public DemoDashboardService(
            AcademicCourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            AlertRepository alertRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public DemoDashboardResult getDashboard(UUID teacherUserId) {

        AcademicCourse course = courseRepository.findByTeacherUserId(teacherUserId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo course not found. Run demo sync first."));

        Activity activity = activityRepository
                .findLatestByCourseId(course.getId())
                .orElseThrow(() -> new IllegalStateException("No activity found for demo course."));

        List<Alert> openAlerts = alertRepository.findByCourseIdAndStatus(course.getId(), AlertStatus.OPEN);

        List<Alert> resolvedAlerts = alertRepository.findByCourseIdAndStatus(course.getId(), AlertStatus.RESOLVED);

        Map<UUID, Alert> alertsByStudent =
                openAlerts.stream().collect(Collectors.toMap(Alert::getStudentId, Function.identity()));

        List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());

        List<DemoDashboardResult.StudentSummary> students = enrollments.stream()
                .map(enrollment -> createStudentSummary(enrollment, activity, alertsByStudent))
                .toList();

        int warnings = (int) openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        int critical = (int) openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        return new DemoDashboardResult(
                course.getId(),
                course.getName(),
                course.getSubject(),
                new DemoDashboardResult.ActivitySummary(activity.getId(), activity.getName()),
                new DemoDashboardResult.DashboardSummary(
                        students.size(), openAlerts.size(), warnings, critical, resolvedAlerts.size()),
                students);
    }

    private DemoDashboardResult.StudentSummary createStudentSummary(
            CourseEnrollment enrollment, Activity activity, Map<UUID, Alert> alertsByStudent) {

        Student student =
                studentRepository.findStudentById(enrollment.getStudentId()).orElseThrow();

        Grade grade = gradeRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId())
                .orElseThrow();

        Alert alert = alertsByStudent.get(student.getId());

        String status = alert == null ? "OK" : alert.getSeverity().name();

        return new DemoDashboardResult.StudentSummary(student.getId(), student.getFullName(), grade.getScore(), status);
    }
}
