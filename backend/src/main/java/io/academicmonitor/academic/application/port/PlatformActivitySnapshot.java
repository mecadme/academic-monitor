package io.academicmonitor.academic.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlatformActivitySnapshot(
        String externalId,
        String name,
        BigDecimal maximumScore,
        LocalDate dueDate,
        List<PlatformGradeSnapshot> grades) {

    public PlatformActivitySnapshot {
        grades = grades == null ? List.of() : List.copyOf(grades);
    }
}
