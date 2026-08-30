package io.academicmonitor.integration.idukay.period;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayCoursePeriodClient {

    private static final String ENDPOINT = "custom_year_course";

    private final IdukayApiClient apiClient;

    public IdukayCoursePeriodClient(IdukayApiClient apiClient) {

        this.apiClient = apiClient;
    }

    public IdukayCustomYearDto findCustomYear(IdukayAuthenticatedSession session, String courseExternalId) {

        String courseId = requireText(courseExternalId, "courseExternalId");

        IdukayCoursePeriodResponse result =
                apiClient.get(session, ENDPOINT, Map.of("_id", courseId), IdukayCoursePeriodResponse.class);

        if (result == null) {
            throw new IdukayApiException("Idukay returned an empty course period response");
        }

        if (hasErrors(result.errors())) {
            throw new IdukayApiException("Idukay rejected the course period request");
        }

        IdukayCustomYearDto customYear = result.response();

        if (customYear == null) {
            throw new IdukayApiException("Idukay did not return a custom year for the requested course");
        }

        return customYear;
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
