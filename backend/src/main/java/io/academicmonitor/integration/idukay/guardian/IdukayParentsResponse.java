package io.academicmonitor.integration.idukay.guardian;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record IdukayParentsResponse(JsonNode errors, List<IdukayParentDto> response) {
    public IdukayParentsResponse {
        response = response == null ? List.of() : List.copyOf(response);
    }
}
