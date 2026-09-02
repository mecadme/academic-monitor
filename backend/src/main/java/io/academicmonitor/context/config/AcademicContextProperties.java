package io.academicmonitor.context.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.bootstrap")
public record AcademicContextProperties(
        @NotBlank String userEmail, @NotBlank String institutionName, @NotBlank String timezone) {}
