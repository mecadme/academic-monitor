package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record IdukayLoginContextsRequest(@JsonProperty("attempt_id") String attemptId) {}
