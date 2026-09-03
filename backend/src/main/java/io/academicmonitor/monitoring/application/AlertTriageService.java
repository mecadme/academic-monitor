package io.academicmonitor.monitoring.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertTriageService {

    private final AcademicCourseRepository courseRepository;
    private final AlertRepository alertRepository;

    public AlertTriageService(AcademicCourseRepository courseRepository, AlertRepository alertRepository) {
        this.courseRepository = courseRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public void acknowledge(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Alert alert = findOwnedOpenAlert(institutionId, teacherUserId, alertId);
        if (alert.acknowledge()) {
            alertRepository.save(alert);
        }
    }

    @Transactional
    public void markPending(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Alert alert = findOwnedOpenAlert(institutionId, teacherUserId, alertId);
        if (alert.markPending()) {
            alertRepository.save(alert);
        }
    }

    private Alert findOwnedOpenAlert(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Objects.requireNonNull(institutionId, "institutionId is required");
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Objects.requireNonNull(alertId, "alertId is required");

        List<AcademicCourse> courses =
                courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);
        Set<UUID> allowedCourseIds = courses.stream()
                .map(AcademicCourse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (allowedCourseIds.isEmpty()) {
            throw new AlertTriageNotFoundException();
        }

        Alert alert = alertRepository
                .findById(alertId)
                .filter(candidate -> institutionId.equals(candidate.getInstitutionId()))
                .filter(candidate -> allowedCourseIds.contains(candidate.getCourseId()))
                .orElseThrow(AlertTriageNotFoundException::new);

        if (!alert.isOpen()) {
            throw new AlertTriageConflictException();
        }

        return alert;
    }
}
