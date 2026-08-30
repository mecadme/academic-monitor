package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record IdukayLoginProfilesRequest(String school, @JsonProperty("attempt_id") String attemptId) {}
