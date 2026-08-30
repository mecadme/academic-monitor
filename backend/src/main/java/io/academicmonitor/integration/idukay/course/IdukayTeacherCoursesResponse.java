package io.academicmonitor.integration.idukay.course;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record IdukayTeacherCoursesResponse(JsonNode errors, List<IdukayTeacherCourseDto> response) {

    public IdukayTeacherCoursesResponse {
        response = response == null ? List.of() : List.copyOf(response);
    }
}
