package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayLoginSchool(@JsonProperty("_id") String id, String name) {}
