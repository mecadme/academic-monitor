package io.academicmonitor.integration.idukay.auth;

import tools.jackson.databind.JsonNode;

record IdukayOauthResponse(JsonNode errors, Response response) {

    record Response(String token, IdukayOauthUser user) {}
}
