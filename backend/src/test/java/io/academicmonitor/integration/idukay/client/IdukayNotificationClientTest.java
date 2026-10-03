package io.academicmonitor.integration.idukay.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRecipient;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayNotificationClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsOnlyTheConfirmedPayloadAndAuthenticatedContext() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> workingProfile = new AtomicReference<>();
        AtomicReference<String> workingSchool = new AtomicReference<>();
        AtomicReference<String> profileType = new AtomicReference<>();
        AtomicReference<String> clientVersion = new AtomicReference<>();
        startServer(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            workingProfile.set(exchange.getRequestHeaders().getFirst("WorkingProfile"));
            workingSchool.set(exchange.getRequestHeaders().getFirst("WorkingSchool"));
            profileType.set(exchange.getRequestHeaders().getFirst("ProfileType"));
            clientVersion.set(exchange.getRequestHeaders().getFirst("ClientVersion"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        int status = client().post(session(), notification());

        assertEquals(200, status);
        assertEquals("synthetic-token", authorization.get());
        assertEquals("profile-test-001", workingProfile.get());
        assertEquals("school-test-001", workingSchool.get());
        assertEquals("staff", profileType.get());
        assertEquals("12.0.2", clientVersion.get());
        assertEquals(
                "{\"recipients\":[{\"user\":\"guardian-user-id\",\"read\":false}],\"subject\":\"Subject\",\"content\":\"<p>Content</p>\",\"sent\":true,\"sender\":\"authenticated-user-id\"}",
                body.get());
        assertFalse(body.get().contains("selected_users"));
        assertFalse(body.get().contains("guardian-profile-id"));
        assertFalse(body.get().contains("student-user-id"));
    }

    @Test
    void treatsEvery2xxStatusAsSuccess() throws Exception {
        startServer(exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        assertEquals(204, client().post(session(), notification()));
    }

    @Test
    void doesNotRetryA503NotificationPost() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });

        IdukayApiException exception =
                assertThrows(IdukayApiException.class, () -> client().post(session(), notification()));

        assertEquals(503, exception.getStatusCode());
        assertEquals(1, requests.get());
    }

    private IdukayNotificationClient client() {
        return new IdukayNotificationClient("12.0.2");
    }

    private IdukayAuthenticatedSession session() {
        IdukaySessionContext context = new IdukaySessionContext(
                "year-test-001",
                "school-test-001",
                "organization-test-001",
                null,
                "profile-test-001",
                "staff",
                "-05:00",
                "permissions;");
        return IdukayAuthenticatedSession.create(
                "synthetic-token",
                "authenticated-user-id",
                context,
                RestClient.builder()
                        .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/")
                        .build());
    }

    private static IdukayNotificationRequest notification() {
        return new IdukayNotificationRequest(
                List.of(new IdukayNotificationRecipient("guardian-user-id", false)),
                "Subject",
                "<p>Content</p>",
                true,
                "authenticated-user-id");
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/notifications", handler);
        server.start();
    }
}
