package io.academicmonitor.monitoring.application;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AlertEvaluationService {

    private static final String LOW_GRADE_RULE = "LOW_GRADE";

    private static final BigDecimal CRITICAL_LIMIT = new BigDecimal("5.00");

    private static final BigDecimal WARNING_LIMIT = new BigDecimal("7.00");

    private final AlertRepository alertRepository;

    public AlertEvaluationService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void evaluate(UUID institutionId, UUID courseId, UUID activityId, UUID studentId, BigDecimal score) {

        Optional<Alert> existing = alertRepository.findByActivityIdAndStudentIdAndRuleCodeAndStatus(
                activityId, studentId, LOW_GRADE_RULE, AlertStatus.OPEN);

        if (score.compareTo(WARNING_LIMIT) > 0) {
            existing.ifPresent(alert -> {
                alert.resolve();
                alertRepository.save(alert);
            });

            return;
        }

        AlertSeverity severity = score.compareTo(CRITICAL_LIMIT) <= 0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;

        if (existing.isPresent()) {
            Alert alert = existing.orElseThrow();

            alert.refresh(severity, score);
            alertRepository.save(alert);

            return;
        }

        Alert alert = new Alert(institutionId, courseId, activityId, studentId, LOW_GRADE_RULE, severity, score);

        alertRepository.save(alert);
    }
}
