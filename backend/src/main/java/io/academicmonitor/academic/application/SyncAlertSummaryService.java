package io.academicmonitor.academic.application;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyncAlertSummaryService {

    private final AlertRepository alertRepository;

    public SyncAlertSummaryService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    Summary summarize(UUID courseId, List<UUID> processedActivityIds) {

        if (processedActivityIds.isEmpty()) {
            return Summary.empty();
        }

        List<Alert> openAlerts = alertRepository.findByCourseIdAndStatusAndActivityIdIn(
                courseId, AlertStatus.OPEN, processedActivityIds);

        long warnings = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        long critical = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        return new Summary(openAlerts.size(), warnings, critical);
    }

    record Summary(int openAlerts, long warnings, long critical) {

        private static Summary empty() {
            return new Summary(0, 0, 0);
        }
    }
}
