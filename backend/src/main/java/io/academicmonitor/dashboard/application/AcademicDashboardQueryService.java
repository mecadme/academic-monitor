package io.academicmonitor.dashboard.application;

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
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicDashboardQueryService {

    private final AcademicCourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AlertRepository alertRepository;

    public AcademicDashboardQueryService(
            AcademicCourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            ActivityRepository activityRepository,
            AcademicYearRepository academicYearRepository,
            AlertRepository alertRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.activityRepository = activityRepository;
        this.academicYearRepository = academicYearRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public AcademicDashboardResponse getDashboard(UUID institutionId, UUID teacherUserId) {
        Objects.requireNonNull(institutionId, "institutionId is required");
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");

        List<AcademicCourse> courses =
                courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);

        if (courses.isEmpty()) {
            return emptyDashboard(institutionId, teacherUserId);
        }

        Set<UUID> courseIds = courses.stream().map(AcademicCourse::getId).collect(Collectors.toUnmodifiableSet());

        List<CourseEnrollment> enrollments = enrollmentRepository.findEnrollmentsByCourseIdIn(courseIds);
        List<Activity> activities = activityRepository.findActivitiesByCourseIdIn(courseIds);
        List<Alert> openAlerts =
                alertRepository.findByInstitutionIdAndCourseIdInAndStatus(institutionId, courseIds, AlertStatus.OPEN);

        Map<UUID, Long> studentsByCourse = enrollments.stream()
                .collect(Collectors.groupingBy(
                        CourseEnrollment::getCourseId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(CourseEnrollment::getStudentId, Collectors.toSet()),
                                studentIds -> (long) studentIds.size())));

        Map<UUID, Long> activitiesByCourse = countByCourse(activities, Activity::getCourseId);
        Map<UUID, AlertCounts> alertsByCourse = countAlertsByCourse(openAlerts);
        Map<UUID, String> academicYearsById = loadAcademicYears(institutionId, courses);

        List<AcademicDashboardResponse.CourseSummary> courseSummaries = courses.stream()
                .sorted(Comparator.comparing(AcademicCourse::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AcademicCourse::getId))
                .map(course -> toCourseSummary(
                        course, studentsByCourse, activitiesByCourse, alertsByCourse, academicYearsById))
                .toList();

        long distinctStudents = enrollments.stream()
                .map(CourseEnrollment::getStudentId)
                .distinct()
                .count();

        AlertCounts totals = countAlerts(openAlerts);

        return new AcademicDashboardResponse(
                institutionId,
                teacherUserId,
                new AcademicDashboardResponse.DashboardSummary(
                        courses.size(),
                        distinctStudents,
                        activities.size(),
                        totals.openAlerts(),
                        totals.warnings(),
                        totals.critical()),
                courseSummaries);
    }

    private Map<UUID, String> loadAcademicYears(UUID institutionId, List<AcademicCourse> courses) {
        Set<UUID> academicYearIds = courses.stream()
                .map(AcademicCourse::getAcademicYearId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (academicYearIds.isEmpty()) {
            return Map.of();
        }

        return academicYearRepository.findByInstitutionIdAndIdIn(institutionId, academicYearIds).stream()
                .collect(Collectors.toUnmodifiableMap(AcademicYear::getId, AcademicYear::getName));
    }

    private static AcademicDashboardResponse.CourseSummary toCourseSummary(
            AcademicCourse course,
            Map<UUID, Long> studentsByCourse,
            Map<UUID, Long> activitiesByCourse,
            Map<UUID, AlertCounts> alertsByCourse,
            Map<UUID, String> academicYearsById) {
        AlertCounts alerts = alertsByCourse.getOrDefault(course.getId(), AlertCounts.empty());
        String academicYear =
                course.getAcademicYearId() == null ? null : academicYearsById.get(course.getAcademicYearId());

        return new AcademicDashboardResponse.CourseSummary(
                course.getId(),
                course.getName(),
                course.getSubject(),
                academicYear,
                studentsByCourse.getOrDefault(course.getId(), 0L),
                activitiesByCourse.getOrDefault(course.getId(), 0L),
                alerts.openAlerts(),
                alerts.warnings(),
                alerts.critical());
    }

    private static <T> Map<UUID, Long> countByCourse(Collection<T> values, Function<T, UUID> courseId) {
        return values.stream().collect(Collectors.groupingBy(courseId, Collectors.counting()));
    }

    private static Map<UUID, AlertCounts> countAlertsByCourse(List<Alert> alerts) {
        Map<UUID, AlertCounts> counts = new HashMap<>();

        for (Alert alert : alerts) {
            counts.merge(alert.getCourseId(), AlertCounts.from(alert), AlertCounts::add);
        }

        return Map.copyOf(counts);
    }

    private static AlertCounts countAlerts(List<Alert> alerts) {
        AlertCounts counts = AlertCounts.empty();

        for (Alert alert : alerts) {
            counts = counts.add(AlertCounts.from(alert));
        }

        return counts;
    }

    private static AcademicDashboardResponse emptyDashboard(UUID institutionId, UUID teacherUserId) {
        return new AcademicDashboardResponse(
                institutionId,
                teacherUserId,
                new AcademicDashboardResponse.DashboardSummary(0, 0, 0, 0, 0, 0),
                List.of());
    }

    private record AlertCounts(long openAlerts, long warnings, long critical) {

        private static AlertCounts empty() {
            return new AlertCounts(0, 0, 0);
        }

        private static AlertCounts from(Alert alert) {
            return switch (alert.getSeverity()) {
                case WARNING -> new AlertCounts(1, 1, 0);
                case CRITICAL -> new AlertCounts(1, 0, 1);
            };
        }

        private AlertCounts add(AlertCounts other) {
            return new AlertCounts(openAlerts + other.openAlerts, warnings + other.warnings, critical + other.critical);
        }
    }
}
