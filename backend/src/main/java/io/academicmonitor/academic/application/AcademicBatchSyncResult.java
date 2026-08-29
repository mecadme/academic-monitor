package io.academicmonitor.academic.application;

import java.util.List;

public record AcademicBatchSyncResult(
    List<AcademicSyncResult> courses) {

    public AcademicBatchSyncResult {
        courses = courses == null
            ? List.of()
            : List.copyOf(courses);
    }

    public int coursesProcessed() {
        return courses.size();
    }

    public int gradesProcessed() {
        return courses.stream()
            .mapToInt(AcademicSyncResult::gradesProcessed)
            .sum();
    }

    public int openAlerts() {
        return courses.stream()
            .mapToInt(AcademicSyncResult::openAlerts)
            .sum();
    }

    public long warnings() {
        return courses.stream()
            .mapToLong(AcademicSyncResult::warnings)
            .sum();
    }

    public long critical() {
        return courses.stream()
            .mapToLong(AcademicSyncResult::critical)
            .sum();
    }
}
