package io.academicmonitor.integration.idukay.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayParentReferenceDto(@JsonProperty("_id") String id) {}
