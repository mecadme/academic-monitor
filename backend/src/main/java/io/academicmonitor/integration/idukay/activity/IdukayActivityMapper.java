package io.academicmonitor.integration.idukay.activity;

import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class IdukayActivityMapper {

    /*
     * Temporary compatibility assumption.
     *
     * The captured Idukay activity payload does not expose an
     * explicit maximum score. Academic Monitor currently requires
     * one in PlatformActivitySnapshot.
     *
     * Replace this once the course grading scale is modeled.
     */
    private static final BigDecimal TEMPORARY_MAXIMUM_SCORE = BigDecimal.TEN;

    private IdukayActivityMapper() {}

    public static PlatformActivitySnapshot toSnapshot(IdukayActivityDto activity) {

        if (activity == null) {
            throw new IllegalArgumentException("activity is required");
        }

        String externalId = requireText(activity.id(), "activity._id");

        String name = requireText(activity.name(), "activity.name");

        LocalDate activityDate = toLocalDate(activity.date());

        return new PlatformActivitySnapshot(externalId, name, TEMPORARY_MAXIMUM_SCORE, activityDate, List.of());
    }

    private static LocalDate toLocalDate(Long epochSeconds) {

        if (epochSeconds == null) {
            return null;
        }

        if (epochSeconds < 0) {
            throw new IdukayApiException("Idukay activity date cannot be negative");
        }

        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IdukayApiException("Idukay response did not contain " + field);
        }

        return value.trim();
    }
}
