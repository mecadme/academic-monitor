package io.academicmonitor.integration.idukay.auth;

import org.springframework.web.client.RestClient;

public final class IdukayAuthenticatedSession {

    private final String authorizationToken;
    private final IdukaySessionContext context;
    private final RestClient restClient;

    IdukayAuthenticatedSession(String authorizationToken, IdukaySessionContext context, RestClient restClient) {

        this.authorizationToken = requireText(authorizationToken, "authorizationToken");

        this.context = context;
        this.restClient = restClient;
    }

    String authorizationToken() {
        return authorizationToken;
    }

    public IdukaySessionContext context() {
        return context;
    }

    RestClient restClient() {
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
