package io.academicmonitor.demo.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DemoDashboardResult(
        UUID courseId,
        String courseName,
        String subject,
        ActivitySummary activity,
        DashboardSummary summary,
        List<StudentSummary> students) {

    public record ActivitySummary(UUID id, String name) {}

    public record DashboardSummary(int totalStudents, int openAlerts, int warnings, int critical, int resolvedAlerts) {}

    public record StudentSummary(UUID id, String name, BigDecimal score, String status) {}
}
