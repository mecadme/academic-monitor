package io.academicmonitor.integration.idukay.guardian;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.course.IdukayStudentDto;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayStudentDetailsClient {
    private static final String ENDPOINT = "students";
    private final IdukayApiClient apiClient;

    public IdukayStudentDetailsClient(IdukayApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IdukayStudentDto findStudent(IdukayAuthenticatedSession session, String studentExternalId) {
        String studentId = requireText(studentExternalId);
        IdukayStudentDetailsResponse result = apiClient.get(
                session,
                ENDPOINT,
                Map.of("__payments", "true", "_id", studentId, "populate", "true"),
                IdukayStudentDetailsResponse.class);
        if (result == null) throw new IdukayApiException("Idukay returned an empty student detail response");
        if (hasErrors(result.errors())) throw new IdukayApiException("Idukay rejected the student detail request");
        return result.response().isEmpty() ? null : result.response().getFirst();
    }

    private static boolean hasErrors(JsonNode errors) {
        if (errors == null || errors.isNull() || errors.isMissingNode()) return false;
        if (errors.isArray() || errors.isObject()) return errors.size() > 0;
        return !errors.isTextual() || !errors.asText().isBlank();
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("studentExternalId is required");
        return value.trim();
    }
}
