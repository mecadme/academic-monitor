package io.academicmonitor.integration.idukay.activity;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayCourseActivitiesClient {

    private static final String ENDPOINT = "teacher_courses";

    private static final String SELECT = "activities";

    private final IdukayApiClient apiClient;

    public IdukayCourseActivitiesClient(IdukayApiClient apiClient) {

        this.apiClient = apiClient;
    }

    public List<IdukayActivityDto> findActivities(IdukayAuthenticatedSession session, String courseExternalId) {

        String courseId = requireText(courseExternalId, "courseExternalId");

        IdukayCourseActivitiesResponse result = apiClient.get(
                session, ENDPOINT, Map.of("_id", courseId, "select", SELECT), IdukayCourseActivitiesResponse.class);

        if (result == null) {
            throw new IdukayApiException("Idukay returned an empty course activities response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayApiException("Idukay rejected the course activities request");
        }

        if (result.response().isEmpty()) {
            throw new IdukayApiException("Idukay did not return the requested course");
        }

        if (result.response().size() > 1) {
            throw new IdukayApiException("Idukay returned more than one course for the requested id");
        }

        return result.response().getFirst().activities();
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

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
