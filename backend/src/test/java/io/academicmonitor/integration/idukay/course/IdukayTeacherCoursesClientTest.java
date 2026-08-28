package io.academicmonitor.integration.idukay.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentSnapshot;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.client.IdukayApiClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdukayTeacherCoursesClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void findsTeacherCoursesAndMapsCourseMetadataAndStudents() throws Exception {

        AtomicReference<String> select = new AtomicReference<>();

        AtomicReference<String> populate = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/api/teacher_courses", exchange -> {
            select.set(queryParam(exchange, "select"));

            populate.set(queryParam(exchange, "populate"));

            sendJson(
                    exchange,
                    """
                    {
                      "errors": [],
                      "response": [
                        {
                          "_id": "course-test-001",
                          "name": "1.º BGU A",
                          "reference_name": "1 BGU A",
                          "code": "1BGUA-FIS",
                          "subject": {
                            "_id": "subject-test-001",
                            "name": "Física"
                          },
                          "students": [
                            {
                              "_id": "student-test-001",
                              "relational_data": {
                                "name": {
                                  "show": "PEREZ LOPEZ, ANA MARIA",
                                  "order": "perez lopez, ana maria"
                                }
                              }
                            },
                            {
                              "_id": "student-test-002",
                              "relational_data": {
                                "name": {
                                  "show": "TORRES VEGA, LUIS",
                                  "order": "torres vega, luis"
                                }
                              }
                            }
                          ]
                        },
                        {
                          "_id": "course-test-002",
                          "name": "2.º BGU B",
                          "reference_name": "2 BGU B",
                          "code": "2BGUB-FIS",
                          "subject": {
                            "_id": "subject-test-001",
                            "name": "Física"
                          },
                          "students": []
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

        IdukayTeacherCoursesClient coursesClient = new IdukayTeacherCoursesClient(apiClient);

        List<IdukayTeacherCourseDto> courses = coursesClient.findTeacherCourses(session);

        assertEquals("name reference_name code subject students", select.get());

        assertEquals("{\"students\":\"relational_data\",\"subject\":\"name\"}", populate.get());

        assertEquals(2, courses.size());

        IdukayTeacherCourseDto first = courses.getFirst();

        assertEquals("course-test-001", first.id());

        assertEquals("1.º BGU A", first.name());

        assertEquals("1 BGU A", first.referenceName());

        assertEquals("1BGUA-FIS", first.code());

        assertEquals("Física", first.subject().name());

        assertEquals(2, first.students().size());

        assertEquals("student-test-001", first.students().getFirst().id());

        assertEquals(
                "PEREZ LOPEZ, ANA MARIA",
                first.students().getFirst().relationalData().name().show());

        PlatformCourseSnapshot snapshot = IdukayCourseMapper.toSnapshot(first);

        assertEquals("course-test-001", snapshot.externalId());

        assertEquals("1.º BGU A", snapshot.name());

        assertEquals("Física", snapshot.subject());

        assertTrue(snapshot.activities().isEmpty());

        assertEquals(2, snapshot.students().size());

        PlatformStudentSnapshot firstStudent = snapshot.students().getFirst();

        assertEquals("student-test-001", firstStudent.externalId());

        assertEquals("ANA MARIA", firstStudent.firstName());

        assertEquals("PEREZ LOPEZ", firstStudent.lastName());

        PlatformStudentSnapshot secondStudent = snapshot.students().get(1);

        assertEquals("student-test-002", secondStudent.externalId());

        assertEquals("LUIS", secondStudent.firstName());

        assertEquals("TORRES VEGA", secondStudent.lastName());

        IdukayTeacherCourseDto second = courses.get(1);

        assertTrue(second.students().isEmpty());
    }

    @Test
    void mapperPreservesStudentDisplayNameWhenCommaIsMissing() {

        IdukayTeacherCourseDto course = new IdukayTeacherCourseDto(
                "course-test-001",
                "Curso Demo",
                null,
                null,
                new IdukaySubjectDto("subject-test-001", "Física"),
                List.of(new IdukayStudentDto(
                        "student-test-001",
                        new IdukayStudentRelationalDataDto(new IdukayStudentNameDto("ANA MARIA PEREZ", null)))));

        PlatformCourseSnapshot snapshot = IdukayCourseMapper.toSnapshot(course);

        assertEquals(1, snapshot.students().size());

        PlatformStudentSnapshot student = snapshot.students().getFirst();

        assertEquals("student-test-001", student.externalId());

        assertEquals("ANA MARIA PEREZ", student.firstName());

        assertEquals("", student.lastName());
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
