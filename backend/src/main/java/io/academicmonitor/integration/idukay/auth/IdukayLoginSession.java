package io.academicmonitor.integration.idukay.auth;

import org.springframework.web.client.RestClient;

public final class IdukayLoginSession {

    private final String attemptId;
    private final RestClient restClient;

    IdukayLoginSession(String attemptId, RestClient restClient) {

        this.attemptId = attemptId;
        this.restClient = restClient;
    }

    public String attemptId() {
        return attemptId;
    }

    RestClient restClient() {
        return restClient;
    }
}
