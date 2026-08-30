package io.academicmonitor.integration.idukay.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayApiClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getAddsAuthenticationAndSessionHeaders() throws Exception {

        AtomicReference<String> authorization = new AtomicReference<>();

        AtomicReference<String> workingYear = new AtomicReference<>();

        AtomicReference<String> workingSchool = new AtomicReference<>();

        AtomicReference<String> workingOrganization = new AtomicReference<>();

        AtomicReference<String> workingProfile = new AtomicReference<>();

        AtomicReference<String> profileType = new AtomicReference<>();

        AtomicReference<String> timeZone = new AtomicReference<>();

        AtomicReference<String> acceptedPermissions = new AtomicReference<>();

        AtomicReference<String> clientVersion = new AtomicReference<>();

        AtomicReference<String> selectedStudent = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/api/test", exchange -> {
            authorization.set(header(exchange, "Authorization"));

            workingYear.set(header(exchange, "WorkingYear"));

            workingSchool.set(header(exchange, "WorkingSchool"));

            workingOrganization.set(header(exchange, "WorkingOrganization"));

            workingProfile.set(header(exchange, "WorkingProfile"));

            profileType.set(header(exchange, "ProfileType"));

            timeZone.set(header(exchange, "TimeZone"));

            acceptedPermissions.set(header(exchange, "AcceptedPermissions"));

            clientVersion.set(header(exchange, "ClientVersion"));

            selectedStudent.set(header(exchange, "SelectedStudent"));

            sendJson(
                    exchange,
                    """
                        {
                          "response": "ok",
                          "errors": []
                        }
                        """);
        });

        server.start();

        RestClient restClient = RestClient.builder().baseUrl(baseUrl()).build();

        IdukaySessionContext context = new IdukaySessionContext(
                "year-test-001",
                "school-test-001",
                "organization-test-001",
                null,
                "profile-test-001",
                "staff",
                "-05:00",
                "growth_plans:manage;");

        IdukayAuthenticatedSession session = IdukayAuthenticatedSession.create("synthetic-token", context, restClient);

        IdukayApiClient client = new IdukayApiClient("12.0.2");

        TestResponse result = client.get(session, "test", TestResponse.class);

        assertEquals("ok", result.response());

        assertEquals("synthetic-token", authorization.get());

        assertEquals("year-test-001", workingYear.get());

        assertEquals("school-test-001", workingSchool.get());

        assertEquals("organization-test-001", workingOrganization.get());

        assertEquals("profile-test-001", workingProfile.get());

        assertEquals("staff", profileType.get());

        assertEquals("-05:00", timeZone.get());

        assertEquals("growth_plans:manage;", acceptedPermissions.get());

        assertEquals("12.0.2", clientVersion.get());

        assertNull(selectedStudent.get());
    }

    private String baseUrl() {

        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
    }

    private static String header(HttpExchange exchange, String name) {

        return exchange.getRequestHeaders().getFirst(name);
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {

        byte[] response = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");

        exchange.sendResponseHeaders(200, response.length);

        exchange.getResponseBody().write(response);

        exchange.close();
    }

    record TestResponse(String response) {}
}
