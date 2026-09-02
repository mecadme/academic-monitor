package io.academicmonitor.dashboard.application;

import java.util.List;
import java.util.UUID;

public record AcademicDashboardResponse(
        UUID institutionId, UUID teacherUserId, DashboardSummary summary, List<CourseSummary> courses) {

    public AcademicDashboardResponse {
        courses = List.copyOf(courses);
    }

    public record DashboardSummary(
            long courses, long students, long activities, long openAlerts, long warnings, long critical) {}

    public record CourseSummary(
            UUID id,
            String name,
            String subject,
            String academicYear,
            long students,
            long activities,
            long openAlerts,
            long warnings,
            long critical) {}
}
