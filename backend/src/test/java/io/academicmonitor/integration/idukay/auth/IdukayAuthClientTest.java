package io.academicmonitor.integration.idukay.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayAuthClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void startLoginSendsExpectedPayloadAndKeepsCookies() throws Exception {

        AtomicReference<String> loginRequestBody = new AtomicReference<>();

        AtomicReference<String> probeCookie = new AtomicReference<>();

        server = createServer();

        server.createContext("/api/login/web", exchange -> {
            loginRequestBody.set(readBody(exchange));

            exchange.getResponseHeaders()
                    .add("Set-Cookie", "login_token_test=" + "synthetic-session;" + " Path=/api;" + " HttpOnly");

            sendJson(
                    exchange,
                    200,
                    """
                    {
                      "errors": [],
                      "response": {
                        "attempt_id": "attempt-test-123"
                      }
                    }
                    """);
        });

        server.createContext("/api/probe", exchange -> {
            probeCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));

            exchange.sendResponseHeaders(204, -1);

            exchange.close();
        });

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession session =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), "school-demo");

        assertEquals("attempt-test-123", session.attemptId());

        String requestBody = loginRequestBody.get();

        assertTrue(requestBody.contains("\"email\":\"teacher@example.test\""));

        assertTrue(requestBody.contains("\"password\":\"ENCODED_PASSWORD\""));

        assertTrue(requestBody.contains("\"subdomain_school\":\"school-demo\""));

        assertFalse(requestBody.contains("temporary-password"));

        /*
         * Verify that the RestClient kept the cookie issued
         * by /login/web and sends it on the next request.
         */
        session.restClient().get().uri("probe").retrieve().toBodilessEntity();

        assertTrue(probeCookie.get().contains("login_token_test=" + "synthetic-session"));
    }

    @Test
    void startLoginRejectsIdukayErrors() throws Exception {

        server = createServer();

        server.createContext(
                "/api/login/web",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [
                        {
                          "code": "invalid_credentials"
                        }
                      ],
                      "response": null
                    }
                    """));

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginException exception = assertThrows(
                IdukayLoginException.class,
                () -> client.startLogin("teacher@example.test", "temporary-password".toCharArray(), "school-demo"));

        assertEquals("Idukay rejected the login request", exception.getMessage());
    }

    @Test
    void startLoginRejectsMissingAttemptId() throws Exception {

        server = createServer();

        server.createContext(
                "/api/login/web",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [],
                      "response": {}
                    }
                    """));

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginException exception = assertThrows(
                IdukayLoginException.class,
                () -> client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null));

        assertEquals("Idukay login response did not contain an attempt id", exception.getMessage());
    }

    private HttpServer createServer() throws IOException {

        return HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    private String baseUrl() {

        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
    }

    private static String readBody(HttpExchange exchange) throws IOException {

        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {

        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");

        exchange.sendResponseHeaders(status, response.length);

        exchange.getResponseBody().write(response);

        exchange.close();
    }
}
