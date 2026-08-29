package io.academicmonitor.integration.idukay.auth;

import tools.jackson.databind.JsonNode;

record IdukayLoginWebResponse(
    JsonNode errors,
    Response response) {

    record Response(String attempt_id) {}
}
