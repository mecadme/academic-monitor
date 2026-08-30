package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record IdukayOauthUser(
        IdukayUserPreferences preferences, @JsonProperty("accepted_permissions") String acceptedPermissions) {}
