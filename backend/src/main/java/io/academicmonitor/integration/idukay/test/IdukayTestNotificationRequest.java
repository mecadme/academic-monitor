package io.academicmonitor.integration.idukay.test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record IdukayTestNotificationRequest(
        @NotNull UUID studentId,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 20_000) String content) {}
