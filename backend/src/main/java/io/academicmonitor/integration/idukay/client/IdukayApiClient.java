package io.academicmonitor.integration.idukay.client;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class IdukayApiClient {

    private static final int GET_MAX_ATTEMPTS = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(IdukayApiClient.class);

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

        return executeGet(() -> session.httpClient()
                .get()
                .uri(normalizedUri)
                .headers(headers -> IdukayRequestHeaders.apply(headers, session, clientVersion))
                .retrieve()
                .body(responseType));
    }

    public <T> T get(
            IdukayAuthenticatedSession session, String path, Map<String, String> queryParams, Class<T> responseType) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        String normalizedPath = requireText(path, "path");

        if (responseType == null) {
            throw new IllegalArgumentException("responseType is required");
        }

        Map<String, String> parameters = queryParams == null ? Map.of() : Map.copyOf(queryParams);

        URI uri = buildUri(normalizedPath, parameters);

        return executeGet(() -> session.httpClient()
                .get()
                .uri(uri)
                .headers(headers -> IdukayRequestHeaders.apply(headers, session, clientVersion))
                .retrieve()
                .body(responseType));
    }

    private <T> T executeGet(Supplier<T> request) {

        for (int attempt = 1; attempt <= GET_MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException exception) {
                if (!isTransientStatus(exception.getStatusCode().value()) || attempt == GET_MAX_ATTEMPTS) {
                    throw responseException(exception);
                }
                logRetry(exception.getStatusCode().value(), attempt);
            } catch (ResourceAccessException exception) {
                if (attempt == GET_MAX_ATTEMPTS) {
                    throw communicationException(exception);
                }
                logRetry(exception.getClass().getSimpleName(), attempt);
            } catch (RestClientException exception) {
                throw communicationException(exception);
            }
            waitBeforeRetry(attempt);
        }
        throw new IllegalStateException("GET retry attempts were exhausted unexpectedly");
    }

    private static boolean isTransientStatus(int statusCode) {

        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private static IdukayApiException responseException(RestClientResponseException exception) {

        return new IdukayApiException(
                "Idukay API request failed with HTTP "
                        + exception.getStatusCode().value(),
                exception.getStatusCode().value(),
                exception);
    }

    private static IdukayApiException communicationException(RestClientException exception) {

        return new IdukayApiException("Unable to communicate with Idukay API", exception);
    }

    private static void logRetry(Object technicalError, int attempt) {

        LOGGER.warn(
                "Idukay transient GET failure ({}), attempt {}/{}; retrying",
                technicalError,
                attempt,
                GET_MAX_ATTEMPTS);
    }

    private static void waitBeforeRetry(int attempt) {

        try {
            Thread.sleep(attempt == 1 ? 250 : 750);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdukayApiException("Interrupted while retrying Idukay GET request", exception);
        }
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }

    private static URI buildUri(String path, Map<String, String> queryParams) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);

        queryParams.forEach(builder::queryParam);

        return builder.build().encode().toUri();
    }
}
