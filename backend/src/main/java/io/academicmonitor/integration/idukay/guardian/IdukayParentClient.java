package io.academicmonitor.integration.idukay.guardian;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class IdukayParentClient {
    private static final String ENDPOINT = "parents";
    private final IdukayApiClient apiClient;

    public IdukayParentClient(IdukayApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IdukayParentDto findParent(IdukayAuthenticatedSession session, String parentExternalId) {
        String parentId = requireText(parentExternalId);
        IdukayParentsResponse result =
                apiClient.get(session, ENDPOINT, Map.of("_id", parentId), IdukayParentsResponse.class);
        if (result == null) throw new IdukayApiException("Idukay returned an empty parent detail response");
        if (hasErrors(result.errors())) throw new IdukayApiException("Idukay rejected the parent detail request");
        return result.response().isEmpty() ? null : result.response().getFirst();
    }

    private static boolean hasErrors(JsonNode errors) {
        if (errors == null || errors.isNull() || errors.isMissingNode()) return false;
        if (errors.isArray() || errors.isObject()) return errors.size() > 0;
        return !errors.isTextual() || !errors.asText().isBlank();
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("parentExternalId is required");
        return value.trim();
    }
}
