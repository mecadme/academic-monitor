package io.academicmonitor.integration.idukay.activity;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record IdukayCourseActivitiesResponse(JsonNode errors, List<IdukayCourseActivitiesDto> response) {

    public IdukayCourseActivitiesResponse {
        response = response == null ? List.of() : List.copyOf(response);
    }
}
