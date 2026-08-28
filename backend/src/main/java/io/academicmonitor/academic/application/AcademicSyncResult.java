package io.academicmonitor.academic.application;

import java.util.UUID;

public record AcademicSyncResult(
        UUID courseId,
        String courseName,
        int students,
        int gradesProcessed,
        int openAlerts,
        long warnings,
        long critical) {}
