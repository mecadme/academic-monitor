package io.academicmonitor.integration.idukay.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayActivityScoreDto(
        @JsonProperty("student") String studentId,
        BigDecimal score,
        @JsonProperty("updated_at") Long updatedAt,
        @JsonProperty("created_at") Long createdAt) {}
