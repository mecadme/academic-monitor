package io.academicmonitor.integration.idukay.auth;

import tools.jackson.databind.JsonNode;

record IdukayLoginContextsResponse(JsonNode errors, IdukayLoginContexts response) {}
