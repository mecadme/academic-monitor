package io.academicmonitor.academic.application.port;

import java.math.BigDecimal;
import java.time.Instant;

public record PlatformGradeSnapshot(String studentExternalId, BigDecimal score, Instant recordedAt) {}
