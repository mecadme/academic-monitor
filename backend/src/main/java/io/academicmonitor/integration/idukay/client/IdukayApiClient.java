package io.academicmonitor.integration.idukay.client;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdukayApiClient {

    private final String clientVersion;

    public IdukayApiClient(@Value("${app.idukay.client-version:12.0.2}") String clientVersion) {

        this.clientVersion = requireText(clientVersion, "clientVersion");
    }

    public <T> T get(IdukayAuthenticatedSession session, String uri, Class<T> responseType) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        String normalizedUri = requireText(uri, "uri");

        if (responseType == null) {
            throw new IllegalArgumentException("responseType is required");
        }

        try {
            return session.httpClient()
                    .get()
                    .uri(normalizedUri)
                    .headers(headers -> IdukayRequestHeaders.apply(headers, session, clientVersion))
                    .retrieve()
                    .body(responseType);

        } catch (RestClientResponseException exception) {

            throw new IdukayApiException(
                    "Idukay API request failed with HTTP "
                            + exception.getStatusCode().value(),
                    exception);

        } catch (RestClientException exception) {

            throw new IdukayApiException("Unable to communicate with Idukay API", exception);
        }
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
