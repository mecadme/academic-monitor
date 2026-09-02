package io.academicmonitor.monitoring.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
public class AlertInboxQueryService {

    private static final Comparator<AlertInboxResponse.AlertItem> ALERT_ORDER = Comparator.comparingInt(
                    (AlertInboxResponse.AlertItem alert) -> severityOrder(alert.severity()))
            .thenComparing(AlertInboxResponse.AlertItem::score)
            .thenComparing(alert -> alert.course().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(alert -> alert.student().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(alert -> alert.activity().name(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AlertInboxResponse.AlertItem::id);

    private final AcademicCourseRepository courseRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AlertRepository alertRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;

    public AlertInboxQueryService(
            AcademicCourseRepository courseRepository,
            AcademicPeriodRepository academicPeriodRepository,
            AlertRepository alertRepository,
            ActivityRepository activityRepository,
            StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.alertRepository = alertRepository;
        this.activityRepository = activityRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public AlertInboxResponse getInbox(UUID institutionId, UUID teacherUserId, UUID courseId) {
        return getInbox(institutionId, teacherUserId, courseId, null);
    }

    @Transactional(readOnly = true)
    public AlertInboxResponse getInbox(UUID institutionId, UUID teacherUserId, UUID courseId, UUID academicPeriodId) {
        Objects.requireNonNull(institutionId, "institutionId is required");
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");

        List<AcademicCourse> allowedCourses =
                courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);

        if (!isAllowedPeriod(allowedCourses, academicPeriodId)) {
            return emptyInbox(institutionId, teacherUserId);
        }

        List<AcademicCourse> selectedCourses = selectCourses(allowedCourses, courseId);

        if (selectedCourses.isEmpty()) {
            return emptyInbox(institutionId, teacherUserId);
        }

        Map<UUID, AcademicCourse> coursesById = indexById(selectedCourses, AcademicCourse::getId);
        Set<UUID> courseIds = Set.copyOf(coursesById.keySet());

        List<Alert> alerts =
                alertRepository
                        .findByInstitutionIdAndCourseIdInAndStatus(institutionId, courseIds, AlertStatus.OPEN)
                        .stream()
                        .filter(Alert::isOpen)
                        .filter(alert -> institutionId.equals(alert.getInstitutionId()))
                        .filter(alert -> coursesById.containsKey(alert.getCourseId()))
                        .filter(alert -> alert.getId() != null)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        Alert::getId,
                                        Function.identity(),
                                        (first, ignored) -> first,
                                        LinkedHashMap::new),
                                alertsById -> List.copyOf(alertsById.values())));

        if (alerts.isEmpty()) {
            return emptyInbox(institutionId, teacherUserId);
        }

        Map<UUID, Activity> activitiesById = indexById(
                activityRepository.findActivitiesByCourseIdIn(courseIds).stream()
                        .filter(activity -> coursesById.containsKey(activity.getCourseId()))
                        .filter(activity ->
                                academicPeriodId == null || academicPeriodId.equals(activity.getAcademicPeriodId()))
                        .toList(),
                Activity::getId);

        Set<UUID> studentIds = alerts.stream().map(Alert::getStudentId).collect(Collectors.toUnmodifiableSet());
        Map<UUID, Student> studentsById = indexById(
                studentRepository.findByInstitutionIdAndIdIn(institutionId, studentIds).stream()
                        .filter(student -> institutionId.equals(student.getInstitutionId()))
                        .toList(),
                Student::getId);

        List<AlertInboxResponse.AlertItem> items = alerts.stream()
                .map(alert -> toItem(alert, coursesById, activitiesById, studentsById))
                .filter(Objects::nonNull)
                .sorted(ALERT_ORDER)
                .toList();

        return new AlertInboxResponse(institutionId, teacherUserId, items.size(), items);
    }

    private boolean isAllowedPeriod(List<AcademicCourse> allowedCourses, UUID academicPeriodId) {
        if (academicPeriodId == null) {
            return true;
        }

        Set<UUID> academicYearIds = allowedCourses.stream()
                .map(AcademicCourse::getAcademicYearId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (academicYearIds.isEmpty()) {
            return false;
        }

        return academicPeriodRepository.findByAcademicYearIdIn(academicYearIds).stream()
                .filter(period -> academicYearIds.contains(period.getAcademicYearId()))
                .map(AcademicPeriod::getId)
                .anyMatch(academicPeriodId::equals);
    }

    private static List<AcademicCourse> selectCourses(List<AcademicCourse> allowedCourses, UUID courseId) {
        if (courseId == null) {
            return allowedCourses;
        }

        return allowedCourses.stream()
                .filter(course -> courseId.equals(course.getId()))
                .toList();
    }

    private static AlertInboxResponse.AlertItem toItem(
            Alert alert,
            Map<UUID, AcademicCourse> coursesById,
            Map<UUID, Activity> activitiesById,
            Map<UUID, Student> studentsById) {
        AcademicCourse course = coursesById.get(alert.getCourseId());
        Activity activity = activitiesById.get(alert.getActivityId());
        Student student = studentsById.get(alert.getStudentId());

        if (course == null || activity == null || !course.getId().equals(activity.getCourseId()) || student == null) {
            return null;
        }

        return new AlertInboxResponse.AlertItem(
                alert.getId(),
                alert.getSeverity(),
                alert.getRuleCode(),
                alert.getScoreSnapshot(),
                new AlertInboxResponse.CourseSummary(course.getId(), course.getName(), course.getSubject()),
                new AlertInboxResponse.ActivitySummary(
                        activity.getId(), activity.getName(), activity.getMaxScore(), activity.getDueDate()),
                new AlertInboxResponse.StudentSummary(student.getId(), student.getFullName()));
    }

    private static <T> Map<UUID, T> indexById(List<T> values, Function<T, UUID> idExtractor) {
        return values.stream()
                .filter(value -> idExtractor.apply(value) != null)
                .collect(Collectors.toUnmodifiableMap(idExtractor, Function.identity(), (first, ignored) -> first));
    }

    private static int severityOrder(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case WARNING -> 1;
        };
    }

    private static AlertInboxResponse emptyInbox(UUID institutionId, UUID teacherUserId) {
        return new AlertInboxResponse(institutionId, teacherUserId, 0, List.of());
    }
}
