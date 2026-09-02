package io.academicmonitor.academic.application;

import java.util.UUID;

public record AcademicSyncResult(
        UUID courseId,
        String courseName,
        int students,
        int gradesProcessed,
        int openAlerts,
        long warnings,
        long critical,
        UUID academicPeriodId) {

    public AcademicSyncResult(
            UUID courseId,
            String courseName,
            int students,
            int gradesProcessed,
            int openAlerts,
            long warnings,
            long critical) {
        this(courseId, courseName, students, gradesProcessed, openAlerts, warnings, critical, null);
    }
}
