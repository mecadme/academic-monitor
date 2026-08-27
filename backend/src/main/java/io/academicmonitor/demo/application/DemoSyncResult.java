package io.academicmonitor.demo.application;

import java.util.UUID;

public record DemoSyncResult(
        UUID institutionId,
        UUID teacherUserId,
        UUID courseId,
        String courseName,
        DemoScenario scenario,
        int students,
        int gradesProcessed,
        long openAlerts,
        long warnings,
        long critical) {}
