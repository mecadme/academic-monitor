package io.academicmonitor.integration.idukay.activity;

import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformGradeSnapshot;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class IdukayActivityMapper {

    private IdukayActivityMapper() {}

    public static PlatformActivitySnapshot toSnapshot(IdukayActivityDto activity, BigDecimal maximumScore) {

        if (activity == null) {
            throw new IllegalArgumentException("activity is required");
        }

        if (maximumScore == null) {
            throw new IdukayApiException("Idukay custom year did not contain base_score");
        }

        if (maximumScore.signum() <= 0) {
            throw new IdukayApiException("Idukay base_score must be greater than zero");
        }

        String externalId = requireText(activity.id(), "activity._id");

        String name = requireText(activity.name(), "activity.name");

        LocalDate activityDate = toLocalDate(activity.date());

        List<PlatformGradeSnapshot> grades = activity.scores().stream()
                .filter(score -> score != null && score.score() != null)
                .map(IdukayActivityMapper::toGradeSnapshot)
                .toList();

        return new PlatformActivitySnapshot(externalId, name, maximumScore, activityDate, grades);
    }

    private static PlatformGradeSnapshot toGradeSnapshot(IdukayActivityScoreDto score) {

        if (score == null) {
            throw new IdukayApiException("Idukay activity contained an empty score");
        }

        String studentExternalId = requireText(score.studentId(), "score.student");

        if (score.score() == null) {
            throw new IdukayApiException("Idukay score did not contain a numeric value");
        }

        Instant recordedAt = resolveRecordedAt(score);

        return new PlatformGradeSnapshot(studentExternalId, score.score(), recordedAt);
    }

    private static Instant resolveRecordedAt(IdukayActivityScoreDto score) {

        Long timestamp = score.updatedAt() != null ? score.updatedAt() : score.createdAt();

        if (timestamp == null) {
            return null;
        }

        if (timestamp < 0) {
            throw new IdukayApiException("Idukay score timestamp cannot be negative");
        }

        return Instant.ofEpochSecond(timestamp);
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
