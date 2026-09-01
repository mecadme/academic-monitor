package io.academicmonitor.integration.idukay.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformGradeSnapshot;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayCourseActivitiesClientTest {

    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.TEN;

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
                              "part": "part-test-001",
                              "activity_type": "type-test-001",
                              "description": "",
                              "scores": [
                                {
                                  "student": "student-test-001",
                                  "score": 8.75,
                                  "updated_at": 1758240000
                                },
                                {
                                  "student": "student-test-002",
                                  "score": 6.50,
                                  "created_at": 1758326400
                                }
                              ]
                            },
                            {
                              "_id": "activity-test-002",
                              "name": "Notación científica",
                              "date": 1759276800,
                              "part": "part-test-001",
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

        assertEquals("part-test-001", first.partId());

        assertEquals(2, first.scores().size());

        assertEquals("student-test-001", first.scores().getFirst().studentId());

        assertEquals(new BigDecimal("8.75"), first.scores().getFirst().score());

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(first, MAXIMUM_SCORE, "term-test-001");

        assertEquals("activity-test-001", snapshot.externalId());

        assertEquals("Movimiento rectilíneo", snapshot.name());

        assertEquals(MAXIMUM_SCORE, snapshot.maximumScore());

        assertEquals(LocalDate.of(2025, 9, 18), snapshot.dueDate());

        assertEquals("term-test-001", snapshot.periodExternalId());

        assertEquals(2, snapshot.grades().size());

        PlatformGradeSnapshot firstGrade = snapshot.grades().getFirst();

        assertEquals("student-test-001", firstGrade.studentExternalId());

        assertEquals(new BigDecimal("8.75"), firstGrade.score());

        assertEquals(Instant.ofEpochSecond(1758240000L), firstGrade.recordedAt());

        PlatformGradeSnapshot secondGrade = snapshot.grades().get(1);

        assertEquals("student-test-002", secondGrade.studentExternalId());

        assertEquals(new BigDecimal("6.50"), secondGrade.score());

        assertEquals(Instant.ofEpochSecond(1758326400L), secondGrade.recordedAt());

        IdukayActivityDto second = activities.get(1);

        assertEquals("activity-test-002", second.id());

        assertEquals("part-test-001", second.partId());

        assertTrue(second.scores().isEmpty());
    }

    @Test
    void mapperAllowsMissingActivityDate() {

        IdukayActivityDto activity =
                new IdukayActivityDto("activity-test-001", "Actividad sin fecha", null, null, List.of());

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(activity, MAXIMUM_SCORE, null);

        assertEquals("activity-test-001", snapshot.externalId());

        assertEquals("Actividad sin fecha", snapshot.name());

        assertEquals(MAXIMUM_SCORE, snapshot.maximumScore());

        assertNull(snapshot.dueDate());

        assertTrue(snapshot.grades().isEmpty());
    }

    @Test
    void mapperAllowsScoreWithoutTimestamp() {

        IdukayActivityDto activity = new IdukayActivityDto(
                "activity-test-001",
                "Actividad Demo",
                null,
                null,
                List.of(new IdukayActivityScoreDto("student-test-001", new BigDecimal("9.25"), null, null)));

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(activity, MAXIMUM_SCORE, null);

        assertEquals(1, snapshot.grades().size());

        PlatformGradeSnapshot grade = snapshot.grades().getFirst();

        assertEquals("student-test-001", grade.studentExternalId());

        assertEquals(new BigDecimal("9.25"), grade.score());

        assertNull(grade.recordedAt());
    }

    @Test
    void ignoresScoresWithoutNumericValue() {

        IdukayActivityDto activity = new IdukayActivityDto(
                "activity-001",
                "Actividad de prueba",
                1_700_000_000L,
                null,
                List.of(new IdukayActivityScoreDto("student-001", null, null, null)));

        PlatformActivitySnapshot snapshot = IdukayActivityMapper.toSnapshot(activity, MAXIMUM_SCORE, null);

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
