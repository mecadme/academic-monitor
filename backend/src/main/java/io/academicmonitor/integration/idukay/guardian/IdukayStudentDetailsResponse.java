package io.academicmonitor.integration.idukay.guardian;

import io.academicmonitor.integration.idukay.course.IdukayStudentDto;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record IdukayStudentDetailsResponse(JsonNode errors, List<IdukayStudentDto> response) {
    public IdukayStudentDetailsResponse {
        response = response == null ? List.of() : List.copyOf(response);
    }
}
