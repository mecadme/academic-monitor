package io.academicmonitor.integration.idukay.auth;

import java.util.List;

record IdukayLoginWebResponse(List<Object> errors, Response response) {

    IdukayLoginWebResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    record Response(String attempt_id) {}
}
