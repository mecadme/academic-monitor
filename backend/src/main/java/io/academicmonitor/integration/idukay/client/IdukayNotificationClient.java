package io.academicmonitor.integration.idukay.client;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdukayNotificationClient {

    private final String clientVersion;

    public IdukayNotificationClient(@Value("${app.idukay.client-version:12.0.2}") String clientVersion) {
        if (clientVersion == null || clientVersion.isBlank()) {
            throw new IllegalArgumentException("clientVersion is required");
        }
        this.clientVersion = clientVersion.trim();
    }

    public int post(IdukayAuthenticatedSession session, IdukayNotificationRequest request) {
        if (session == null || request == null) {
            throw new IllegalArgumentException("session and request are required");
        }
        try {
            return session.httpClient()
                    .post()
                    .uri("notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> IdukayRequestHeaders.apply(headers, session, clientVersion))
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
        } catch (RestClientResponseException exception) {
            throw new IdukayApiException(
                    "Idukay notification request failed with HTTP "
                            + exception.getStatusCode().value(),
                    exception.getStatusCode().value(),
                    exception);
        } catch (RestClientException exception) {
            throw new IdukayApiException("Unable to communicate with Idukay notification API", exception);
        }
    }
}
