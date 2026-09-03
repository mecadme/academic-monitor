package io.academicmonitor.monitoring.application;

import io.academicmonitor.monitoring.domain.AlertSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AlertInboxResponse(UUID institutionId, UUID teacherUserId, long total, List<AlertItem> alerts) {

    public AlertInboxResponse {
        alerts = List.copyOf(alerts);
    }

    public record AlertItem(
            UUID id,
            AlertSeverity severity,
            String ruleCode,
            BigDecimal score,
            Instant acknowledgedAt,
            CourseSummary course,
            ActivitySummary activity,
            StudentSummary student) {}

    public record CourseSummary(UUID id, String name, String subject) {}

    public record ActivitySummary(UUID id, String name, BigDecimal maximumScore, LocalDate dueDate) {}

    public record StudentSummary(UUID id, String name) {}
}
