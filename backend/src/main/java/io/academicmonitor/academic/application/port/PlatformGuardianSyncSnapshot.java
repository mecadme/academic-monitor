package io.academicmonitor.academic.application.port;

import java.util.List;

public record PlatformGuardianSyncSnapshot(
        List<PlatformStudentGuardianSnapshot> relationships,
        int warnings,
        int studentsInspected,
        int guardianStudentFetches,
        int guardianStudentWarnings,
        long guardianStudentFetchDurationMs,
        int uniqueParentIds,
        int guardianCacheHits,
        int guardianFetches,
        long guardianSyncDurationMs) {
    public PlatformGuardianSyncSnapshot {
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        if (warnings < 0) throw new IllegalArgumentException("warnings must not be negative");
    }

    public PlatformGuardianSyncSnapshot(List<PlatformStudentGuardianSnapshot> relationships, int warnings) {
        this(relationships, warnings, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public PlatformGuardianSyncSnapshot(
            List<PlatformStudentGuardianSnapshot> relationships,
            int warnings,
            int studentsInspected,
            int uniqueParentIds,
            int guardianCacheHits,
            int guardianFetches,
            long guardianSyncDurationMs) {
        this(
                relationships,
                warnings,
                studentsInspected,
                0,
                0,
                0,
                uniqueParentIds,
                guardianCacheHits,
                guardianFetches,
                guardianSyncDurationMs);
    }

    public static PlatformGuardianSyncSnapshot empty() {
        return new PlatformGuardianSyncSnapshot(List.of(), 0);
    }
}
