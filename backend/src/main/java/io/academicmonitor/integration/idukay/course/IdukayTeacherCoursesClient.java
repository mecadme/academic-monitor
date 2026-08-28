package io.academicmonitor.integration.idukay.course;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayTeacherCoursesClient {

    private static final String ENDPOINT = "teacher_courses";

    private static final String SELECT = "name reference_name code subject";

    private static final String POPULATE = "{\"subject\":\"name\"}";

    private final IdukayApiClient apiClient;

    public IdukayTeacherCoursesClient(IdukayApiClient apiClient) {

        this.apiClient = apiClient;
    }

    public List<IdukayTeacherCourseDto> findTeacherCourses(IdukayAuthenticatedSession session) {

        IdukayTeacherCoursesResponse result = apiClient.get(
                session, ENDPOINT, Map.of("select", SELECT, "populate", POPULATE), IdukayTeacherCoursesResponse.class);

        if (result == null) {
            throw new IdukayApiException("Idukay returned an empty teacher courses response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayApiException("Idukay rejected the teacher courses request");
        }

        return result.response();
    }

    private static boolean hasErrors(JsonNode errors) {

        if (errors == null || errors.isNull() || errors.isMissingNode()) {

            return false;
        }

        if (errors.isArray() || errors.isObject()) {

            return errors.size() > 0;
        }

        if (errors.isTextual()) {
            return !errors.asText().isBlank();
        }

        return true;
    }
}
