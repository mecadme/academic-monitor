package io.academicmonitor.monitoring.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findByActivityIdAndStudentIdAndRuleCodeAndStatus(
            UUID activityId, UUID studentId, String ruleCode, AlertStatus status);

    List<Alert> findByCourseIdAndStatus(UUID courseId, AlertStatus status);
}
