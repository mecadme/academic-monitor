package io.academicmonitor.monitoring.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertInboxResponse;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AlertInboxControllerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ALERT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ACTIVITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private AlertInboxQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AlertInboxQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertInboxController(service))
                .build();
    }

    @Test
    void returnsEnrichedAlertInboxAndPassesOptionalCourseFilter() throws Exception {
        AlertInboxResponse response = new AlertInboxResponse(
                INSTITUTION_ID,
                TEACHER_USER_ID,
                1,
                List.of(new AlertInboxResponse.AlertItem(
                        ALERT_ID,
                        AlertSeverity.CRITICAL,
                        "LOW_GRADE",
                        new BigDecimal("4.50"),
                        new AlertInboxResponse.CourseSummary(
                                COURSE_ID, "Primer Curso A, Bachillerato General Unificado", "Física"),
                        new AlertInboxResponse.ActivitySummary(
                                ACTIVITY_ID,
                                "Movimiento rectilíneo",
                                new BigDecimal("10.00"),
                                LocalDate.of(2026, 1, 15)),
                        new AlertInboxResponse.StudentSummary(STUDENT_ID, "Ana Torres"))));

        when(service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/alerts")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString())
                        .queryParam("courseId", COURSE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value(INSTITUTION_ID.toString()))
                .andExpect(jsonPath("$.teacherUserId").value(TEACHER_USER_ID.toString()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.alerts[0].id").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.alerts[0].ruleCode").value("LOW_GRADE"))
                .andExpect(jsonPath("$.alerts[0].score").value(4.5))
                .andExpect(jsonPath("$.alerts[0].course.id").value(COURSE_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].course.name").value("Primer Curso A, Bachillerato General Unificado"))
                .andExpect(jsonPath("$.alerts[0].course.subject").value("Física"))
                .andExpect(jsonPath("$.alerts[0].activity.id").value(ACTIVITY_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].activity.name").value("Movimiento rectilíneo"))
                .andExpect(jsonPath("$.alerts[0].activity.maximumScore").value(10.0))
                .andExpect(jsonPath("$.alerts[0].activity.dueDate").value("2026-01-15"))
                .andExpect(jsonPath("$.alerts[0].student.id").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].student.name").value("Ana Torres"));

        verify(service).getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_ID);
    }

    @Test
    void requiresInstitutionId() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresTeacherUserId() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isBadRequest());
    }
}
