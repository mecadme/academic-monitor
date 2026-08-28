package io.academicmonitor.integration.idukay.auth;

import tools.jackson.databind.JsonNode;

record IdukayLoginProfilesResponse(JsonNode errors, IdukayLoginProfiles response) {}
