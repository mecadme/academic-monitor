package io.academicmonitor.integration.idukay.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

public final class IdukayAuthenticatedSession {

    private final String authorizationToken;
    private final IdukaySessionContext context;
    private final RestClient restClient;

    IdukayAuthenticatedSession(String authorizationToken, IdukaySessionContext context, RestClient restClient) {

        this.authorizationToken = requireText(authorizationToken, "authorizationToken");

        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }

        if (restClient == null) {
            throw new IllegalArgumentException("restClient is required");
        }

        this.context = context;
        this.restClient = restClient;
    }

    public static IdukayAuthenticatedSession create(
            String authorizationToken, IdukaySessionContext context, RestClient restClient) {

        return new IdukayAuthenticatedSession(authorizationToken, context, restClient);
    }

    public void applyAuthorization(HttpHeaders headers) {

        if (headers == null) {
            throw new IllegalArgumentException("headers are required");
        }

        headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
    }

    public IdukaySessionContext context() {
        return context;
    }

    public RestClient httpClient() {
        return restClient;
    }

    @Override
    public String toString() {
        return "IdukayAuthenticatedSession{" + "authorizationToken=[REDACTED], " + "context=" + context + '}';
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
