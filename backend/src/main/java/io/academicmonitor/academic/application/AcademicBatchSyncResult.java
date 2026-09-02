package io.academicmonitor.academic.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AcademicBatchSyncResult(List<AcademicSyncResult> courses) {

    public AcademicBatchSyncResult {
        courses = courses == null ? List.of() : List.copyOf(courses);
    }

    public int coursesProcessed() {
        return courses.size();
    }

    public int gradesProcessed() {
        return courses.stream().mapToInt(AcademicSyncResult::gradesProcessed).sum();
    }

    public int openAlerts() {
        return courses.stream().mapToInt(AcademicSyncResult::openAlerts).sum();
    }

    public long warnings() {
        return courses.stream().mapToLong(AcademicSyncResult::warnings).sum();
    }

    public long critical() {
        return courses.stream().mapToLong(AcademicSyncResult::critical).sum();
    }

    public UUID academicPeriodId() {
        List<UUID> periodIds = courses.stream()
                .map(AcademicSyncResult::academicPeriodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (periodIds.size() > 1) {
            throw new IllegalStateException("A filtered batch sync resolved more than one academic period");
        }

        return periodIds.isEmpty() ? null : periodIds.getFirst();
    }
}
