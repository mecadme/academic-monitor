package io.academicmonitor.integration.idukay.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayStudentDto(
        @JsonProperty("_id") String id,
        @JsonProperty("relational_data") IdukayStudentRelationalDataDto relationalData) {}
