package io.academicmonitor.integration.idukay.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayCourseActivitiesClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void findsCourseActivitiesAndMapsThemToNeutralSnapshots() throws Exception {

        AtomicReference<String> courseId = new AtomicReference<>();

        AtomicReference<String> select = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/api/teacher_courses", exchange -> {
            courseId.set(queryParam(exchange, "_id"));

            select.set(queryParam(exchange, "select"));

            sendJson(
                    exchange,
                    """
                    {
                      "errors": [],
                      "response": [
                        {
                          "_id": "course-test-001",
                          "activities": [
                            {
                              "_id": "activity-test-001",
                              "name": "Movimiento rectilíneo",
                              "date": 1758153600,
                              "activity_type": "type-test-001",
                              "description": "",
                              "scores": []
                            },
                            {
                              "_id": "activity-test-002",
                              "name": "Notación científica",
                              "date": 1759276800,
                              "activity_type": "type-test-001",
                              "description": "",
                              "scores": []
                            }
                          ]
                        }
                      ]
                    }
                    """);
        });

        server.start();

        RestClient restClient = RestClient.builder().baseUrl(baseUrl()).build();

        IdukaySessionContext context = new IdukaySessionContext(
                "year-test-001", "school-test-001", null, null, "profile-test-001", "staff", "-05:00", null);

        IdukayAuthenticatedSession session = IdukayAuthenticatedSession.create("synthetic-token", context, restClient);

        IdukayApiClient apiClient = new IdukayApiClient("12.0.2");

        IdukayCourseActivitiesClient client = new IdukayCourseActivitiesClient(apiClient);

        List<IdukayActivityDto> activities = client.findActivities(session, "course-test-001");

        assertEquals("course-test-001", courseId.get());

        assertEquals("activities", select.get());

        assertEquals(2, activities.size());

        IdukayActivityDto first = activities.getFirst();

        assertEquals("activity-test-001", first.id());

        assertEquals("Movimiento rectilíneo", first.name());

        assertEquals(1758153600L, first.date());

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(first);

        assertEquals("activity-test-001", snapshot.externalId());

        assertEquals("Movimiento rectilíneo", snapshot.name());

        assertEquals(BigDecimal.TEN, snapshot.maximumScore());

        assertEquals(LocalDate.of(2025, 9, 18), snapshot.dueDate());

        assertTrue(snapshot.grades().isEmpty());
    }

    @Test
    void mapperAllowsMissingActivityDate() {

        IdukayActivityDto activity = new IdukayActivityDto("activity-test-001", "Actividad sin fecha", null);

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(activity);

        assertEquals("activity-test-001", snapshot.externalId());

        assertEquals("Actividad sin fecha", snapshot.name());

        assertEquals(BigDecimal.TEN, snapshot.maximumScore());

        assertEquals(null, snapshot.dueDate());

        assertTrue(snapshot.grades().isEmpty());
    }

    private String baseUrl() {

        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
    }

    private static String queryParam(HttpExchange exchange, String expectedName) {

        String query = exchange.getRequestURI().getRawQuery();

        if (query == null) {
            return null;
        }

        for (String parameter : query.split("&")) {

            int separator = parameter.indexOf('=');

            if (separator < 0) {
                continue;
            }

            String name = decode(parameter.substring(0, separator));

            if (expectedName.equals(name)) {

                return decode(parameter.substring(separator + 1));
            }
        }

        return null;
    }

    private static String decode(String value) {

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {

        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");

        exchange.sendResponseHeaders(200, response.length);

        exchange.getResponseBody().write(response);

        exchange.close();
    }
}
