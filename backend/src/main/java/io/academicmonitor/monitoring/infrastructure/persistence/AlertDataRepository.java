package io.academicmonitor.monitoring.infrastructure.persistence;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AlertDataRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByActivityIdAndStudentIdAndRuleCodeAndStatus(
            UUID activityId, UUID studentId, String ruleCode, AlertStatus status);

    List<Alert> findByCourseIdAndStatus(UUID courseId, AlertStatus status);
}
