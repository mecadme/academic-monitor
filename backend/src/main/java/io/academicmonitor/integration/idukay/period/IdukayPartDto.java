package io.academicmonitor.integration.idukay.period;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayPartDto(
    @JsonProperty("_id") String id,
    String name,
    String abbreviation) {}
