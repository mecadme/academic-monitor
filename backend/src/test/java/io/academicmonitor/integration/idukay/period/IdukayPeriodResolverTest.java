package io.academicmonitor.integration.idukay.period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdukayPeriodResolverTest {

    @Test
    void findsTermContainingRequestedPart() {

        IdukayTermDto firstTerm =
            new IdukayTermDto(
                "term-001",
                "Primer trimestre",
                "T1",
                List.of(
                    new IdukayPartDto(
                        "part-001",
                        "Primer parcial",
                        "P1")));

        IdukayTermDto secondTerm =
            new IdukayTermDto(
                "term-002",
                "Segundo trimestre",
                "T2",
                List.of(
                    new IdukayPartDto(
                        "part-002",
                        "Segundo parcial",
                        "P2")));

        IdukayCustomYearDto customYear =
            new IdukayCustomYearDto(
                "custom-year-001",
                "Año lectivo",
                null,
                null,
                List.of(
                    firstTerm,
                    secondTerm));

        var result =
            IdukayPeriodResolver.findTermByPartId(
                customYear,
                "part-002");

        assertTrue(result.isPresent());

        assertEquals(
            "term-002",
            result.orElseThrow().id());
    }

    @Test
    void returnsEmptyWhenPartDoesNotExist() {

        IdukayCustomYearDto customYear =
            new IdukayCustomYearDto(
                "custom-year-001",
                "Año lectivo",
                null,
                null,
                List.of());

        var result =
            IdukayPeriodResolver.findTermByPartId(
                customYear,
                "unknown-part");

        assertTrue(result.isEmpty());
    }
}
