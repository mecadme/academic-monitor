package io.academicmonitor.academic.application.port;

import java.math.BigDecimal;
import java.util.List;

public record PlatformAcademicYearSnapshot(
        String externalId,
        String name,
        String year,
        BigDecimal baseScore,
        List<PlatformAcademicPeriodSnapshot> periods) {

    public PlatformAcademicYearSnapshot {
        periods = periods == null ? List.of() : List.copyOf(periods);
    }
}
