package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record IdukayOauthRequest(
        IdukayOauthProfile profile, IdukayFingerprint fingerprint, @JsonProperty("attempt_id") String attemptId) {}
