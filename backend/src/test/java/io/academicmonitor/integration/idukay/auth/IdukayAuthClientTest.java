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
import java.util.List;
import java.util.Map;
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

    @Test
    void getAvailableContextsUsesAttemptIdAndLoginCookie() throws Exception {

        AtomicReference<String> contextsRequestBody = new AtomicReference<>();

        AtomicReference<String> contextsCookie = new AtomicReference<>();

        server = createServer();

        server.createContext("/api/login/web", exchange -> {
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

        server.createContext("/api/login/contexts", exchange -> {
            contextsRequestBody.set(readBody(exchange));

            contextsCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));

            sendJson(
                    exchange,
                    200,
                    """
                        {
                          "errors": [],
                          "response": {
                            "user": "user-test-001",
                            "schools": [
                              {
                                "_id": "school-test-001",
                                "name": "Unidad Educativa Demo"
                              }
                            ],
                            "organization": {
                              "_id": "organization-test-001"
                            },
                            "is_admin": false,
                            "profiles": [
                              {
                                "_id": "profile-test-001",
                                "collection_name": "staff"
                              }
                            ]
                          }
                        }
                        """);
        });

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession session =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), "school-demo");

        IdukayLoginContexts contexts = client.getAvailableContexts(session);

        assertTrue(contextsRequestBody.get().contains("\"attempt_id\":\"attempt-test-123\""));

        assertTrue(contextsCookie.get().contains("login_token_test=" + "synthetic-session"));

        assertEquals(1, contexts.schools().size());

        assertEquals("school-test-001", contexts.schools().getFirst().id());

        assertEquals("Unidad Educativa Demo", contexts.schools().getFirst().name());

        assertFalse(contexts.admin());

        assertEquals(1, contexts.profiles().size());

        assertEquals("profile-test-001", contexts.profiles().getFirst().id());

        assertEquals("staff", contexts.profiles().getFirst().collectionName());

        assertEquals("user-test-001", contexts.user().asText());
    }

    @Test
    void getAvailableContextsRejectsIdukayErrors() throws Exception {

        server = createServer();

        server.createContext(
                "/api/login/web",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                        {
                          "errors": [],
                          "response": {
                            "attempt_id": "attempt-test-123"
                          }
                        }
                        """));

        server.createContext(
                "/api/login/contexts",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                        {
                          "errors": [
                            {
                              "code": "invalid_attempt"
                            }
                          ],
                          "response": null
                        }
                        """));

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession session =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null);

        IdukayLoginException exception =
                assertThrows(IdukayLoginException.class, () -> client.getAvailableContexts(session));

        assertEquals("Idukay rejected the login contexts request", exception.getMessage());
    }

    @Test
    void getProfilesBySchoolUsesSchoolAttemptIdAndLoginCookie() throws Exception {

        AtomicReference<String> profilesRequestBody = new AtomicReference<>();

        AtomicReference<String> profilesCookie = new AtomicReference<>();

        server = createServer();

        server.createContext("/api/login/web", exchange -> {
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

        server.createContext("/api/login/profiles", exchange -> {
            profilesRequestBody.set(readBody(exchange));

            profilesCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));

            sendJson(
                    exchange,
                    200,
                    """
                    {
                      "errors": [],
                      "response": {
                        "user": "user-test-001",
                        "profiles": [
                          {
                            "_id": "profile-test-001",
                            "collection_name": "staff"
                          },
                          {
                            "_id": "profile-test-002",
                            "collection_name": "students"
                          }
                        ]
                      }
                    }
                    """);
        });

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession session =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null);

        IdukayLoginProfiles profiles = client.getProfilesBySchool(session, "school-test-001");

        String body = profilesRequestBody.get();

        assertTrue(body.contains("\"school\":\"school-test-001\""));

        assertTrue(body.contains("\"attempt_id\":\"attempt-test-123\""));

        assertTrue(profilesCookie.get().contains("login_token_test=" + "synthetic-session"));

        assertEquals("user-test-001", profiles.user());

        assertEquals(2, profiles.profiles().size());

        assertEquals("profile-test-001", profiles.profiles().getFirst().id());

        assertEquals("staff", profiles.profiles().getFirst().collectionName());

        assertEquals("profile-test-002", profiles.profiles().get(1).id());

        assertEquals("students", profiles.profiles().get(1).collectionName());
    }

    @Test
    void getProfilesBySchoolRejectsIdukayErrors() throws Exception {

        server = createServer();

        server.createContext(
                "/api/login/web",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [],
                      "response": {
                        "attempt_id": "attempt-test-123"
                      }
                    }
                    """));

        server.createContext(
                "/api/login/profiles",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [
                        {
                          "code": "invalid_school"
                        }
                      ],
                      "response": null
                    }
                    """));

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession session =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null);

        IdukayLoginException exception =
                assertThrows(IdukayLoginException.class, () -> client.getProfilesBySchool(session, "school-test-001"));

        assertEquals("Idukay rejected the login profiles request", exception.getMessage());
    }

    @Test
    void completeLoginBuildsAuthenticatedSession() throws Exception {

        AtomicReference<String> oauthRequestBody = new AtomicReference<>();

        AtomicReference<String> oauthCookie = new AtomicReference<>();

        server = createServer();

        server.createContext("/api/login/web", exchange -> {
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

        server.createContext("/api/login/oauth", exchange -> {
            oauthRequestBody.set(readBody(exchange));

            oauthCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));

            sendJson(
                    exchange,
                    200,
                    """
                    {
                      "errors": [],
                      "response": {
                        "token": "synthetic-oauth-token",
                        "user": {
                          "accepted_permissions":
                            "growth_plans:manage;",
                          "preferences": {
                            "working_year": {
                              "_id": "year-test-001"
                            },
                            "working_school": {
                              "_id": "school-test-001"
                            },
                            "working_organization": {
                              "_id": "organization-test-001"
                            },
                            "working_profile": {
                              "_id": "profile-test-001",
                              "collection_name": "staff"
                            },
                            "time_zone": "-05:00"
                          }
                        }
                      }
                    }
                    """);
        });

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession loginSession =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null);

        IdukayOauthProfile profile = new IdukayOauthProfile("staff", "profile-test-001", "user-test-001");

        IdukayFingerprint fingerprint = syntheticFingerprint();

        IdukayAuthenticatedSession session = client.completeLogin(loginSession, profile, fingerprint);

        String body = oauthRequestBody.get();

        assertTrue(body.contains("\"attempt_id\":\"attempt-test-123\""));

        assertTrue(body.contains("\"collection_name\":\"staff\""));

        assertTrue(body.contains("\"_id\":\"profile-test-001\""));

        assertTrue(body.contains("\"user\":\"user-test-001\""));

        assertTrue(body.contains("\"user_agent\":\"Synthetic Browser\""));

        assertTrue(oauthCookie.get().contains("login_token_test=" + "synthetic-session"));

        assertEquals("synthetic-oauth-token", session.authorizationToken());

        IdukaySessionContext context = session.context();

        assertEquals("year-test-001", context.workingYear());

        assertEquals("school-test-001", context.workingSchool());

        assertEquals("organization-test-001", context.workingOrganization());

        assertEquals("profile-test-001", context.workingProfile());

        assertEquals("staff", context.profileType());

        assertEquals("-05:00", context.timeZone());

        assertEquals("growth_plans:manage;", context.acceptedPermissions());

        assertFalse(session.toString().contains("synthetic-oauth-token"));

        assertTrue(session.toString().contains("[REDACTED]"));
    }

    @Test
    void completeLoginRejectsIdukayErrors() throws Exception {

        server = createServer();

        server.createContext(
                "/api/login/web",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [],
                      "response": {
                        "attempt_id": "attempt-test-123"
                      }
                    }
                    """));

        server.createContext(
                "/api/login/oauth",
                exchange -> sendJson(
                        exchange,
                        200,
                        """
                    {
                      "errors": [
                        {
                          "code": "invalid_fingerprint"
                        }
                      ],
                      "response": null
                    }
                    """));

        server.start();

        CryptoJsPasswordEncoder passwordEncoder = mock(CryptoJsPasswordEncoder.class);

        when(passwordEncoder.encode(any(char[].class))).thenReturn("ENCODED_PASSWORD");

        IdukayAuthClient client = new IdukayAuthClient(RestClient.builder(), passwordEncoder, baseUrl());

        IdukayLoginSession loginSession =
                client.startLogin("teacher@example.test", "temporary-password".toCharArray(), null);

        IdukayLoginException exception = assertThrows(
                IdukayLoginException.class,
                () -> client.completeLogin(
                        loginSession,
                        new IdukayOauthProfile("staff", "profile-test-001", "user-test-001"),
                        syntheticFingerprint()));

        assertEquals("Idukay rejected the OAuth request", exception.getMessage());
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

    private IdukayFingerprint syntheticFingerprint() {

        return new IdukayFingerprint(
                "Synthetic Browser",
                "es-EC",
                List.of("es-EC", "es"),
                "SyntheticPlatform",
                8,
                8.0,
                Map.of(
                        "width", 1920,
                        "height", 1080),
                "America/Guayaquil",
                0,
                "synthetic-canvas",
                "synthetic-webgl",
                "synthetic-audio");
    }
}
