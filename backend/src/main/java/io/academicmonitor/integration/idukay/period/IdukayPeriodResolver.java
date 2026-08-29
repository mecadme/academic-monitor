package io.academicmonitor.integration.idukay.period;

import java.util.Optional;

public final class IdukayPeriodResolver {

    private IdukayPeriodResolver() {}

    public static Optional<IdukayTermDto> findTermByPartId(
        IdukayCustomYearDto customYear,
        String partId) {

        if (customYear == null) {
            throw new IllegalArgumentException(
                "customYear is required");
        }

        if (partId == null || partId.isBlank()) {
            return Optional.empty();
        }

        return customYear.terms()
            .stream()
            .filter(term ->
                term.parts()
                    .stream()
                    .anyMatch(part ->
                        partId.equals(part.id())))
            .findFirst();
    }
}
