package io.academicmonitor.monitoring.infrastructure.persistence;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AlertRepositoryAdapter implements AlertRepository {

    private final AlertDataRepository repository;

    AlertRepositoryAdapter(AlertDataRepository repository) {

        this.repository = repository;
    }

    @Override
    public Alert save(Alert alert) {
        return repository.save(alert);
    }

    @Override
    public Optional<Alert> findByActivityIdAndStudentIdAndRuleCodeAndStatus(
            UUID activityId, UUID studentId, String ruleCode, AlertStatus status) {

        return repository.findByActivityIdAndStudentIdAndRuleCodeAndStatus(activityId, studentId, ruleCode, status);
    }

    @Override
    public List<Alert> findByCourseIdAndStatus(UUID courseId, AlertStatus status) {

        return repository.findByCourseIdAndStatus(courseId, status);
    }

    @Override
    public List<Alert> findByCourseIdAndStatusAndActivityIdIn(
            UUID courseId, AlertStatus status, Collection<UUID> activityIds) {

        return repository.findByCourseIdAndStatusAndActivityIdIn(courseId, status, activityIds);
    }

    @Override
    public List<Alert> findByInstitutionIdAndCourseIdInAndStatus(
            UUID institutionId, Collection<UUID> courseIds, AlertStatus status) {
        return repository.findByInstitutionIdAndCourseIdInAndStatus(institutionId, courseIds, status);
    }
}
