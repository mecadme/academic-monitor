package io.academicmonitor.monitoring.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.monitoring.application.AlertAttentionState;
import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertInboxResponse;
import io.academicmonitor.monitoring.application.AlertTriageConflictException;
import io.academicmonitor.monitoring.application.AlertTriageNotFoundException;
import io.academicmonitor.monitoring.application.AlertTriageService;
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
    private static final UUID ACADEMIC_PERIOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333334");
    private static final UUID ALERT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ACTIVITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private AlertInboxQueryService service;
    private AlertTriageService triageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AlertInboxQueryService.class);
        triageService = mock(AlertTriageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertInboxController(service, triageService))
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
                        null,
                        new AlertInboxResponse.CourseSummary(
                                COURSE_ID, "Primer Curso A, Bachillerato General Unificado", "Física"),
                        new AlertInboxResponse.ActivitySummary(
                                ACTIVITY_ID,
                                "Movimiento rectilíneo",
                                new BigDecimal("10.00"),
                                LocalDate.of(2026, 1, 15)),
                        new AlertInboxResponse.StudentSummary(STUDENT_ID, "Ana Torres"))));

        when(service.getInbox(
                        INSTITUTION_ID, TEACHER_USER_ID, COURSE_ID, ACADEMIC_PERIOD_ID, AlertAttentionState.PENDING))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/alerts")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString())
                        .queryParam("courseId", COURSE_ID.toString())
                        .queryParam("academicPeriodId", ACADEMIC_PERIOD_ID.toString())
                        .queryParam("attentionState", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value(INSTITUTION_ID.toString()))
                .andExpect(jsonPath("$.teacherUserId").value(TEACHER_USER_ID.toString()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.alerts[0].id").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.alerts[0].ruleCode").value("LOW_GRADE"))
                .andExpect(jsonPath("$.alerts[0].score").value(4.5))
                .andExpect(jsonPath("$.alerts[0].acknowledgedAt").isEmpty())
                .andExpect(jsonPath("$.alerts[0].course.id").value(COURSE_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].course.name").value("Primer Curso A, Bachillerato General Unificado"))
                .andExpect(jsonPath("$.alerts[0].course.subject").value("Física"))
                .andExpect(jsonPath("$.alerts[0].activity.id").value(ACTIVITY_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].activity.name").value("Movimiento rectilíneo"))
                .andExpect(jsonPath("$.alerts[0].activity.maximumScore").value(10.0))
                .andExpect(jsonPath("$.alerts[0].activity.dueDate").value("2026-01-15"))
                .andExpect(jsonPath("$.alerts[0].student.id").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.alerts[0].student.name").value("Ana Torres"));

        verify(service)
                .getInbox(INSTITUTION_ID, TEACHER_USER_ID, COURSE_ID, ACADEMIC_PERIOD_ID, AlertAttentionState.PENDING);
    }

    @Test
    void omittedAttentionStateUsesAllOpenAlertsForBackwardCompatibility() throws Exception {
        AlertInboxResponse empty = new AlertInboxResponse(INSTITUTION_ID, TEACHER_USER_ID, 0, List.of());
        when(service.getInbox(INSTITUTION_ID, TEACHER_USER_ID, null, null, AlertAttentionState.ALL))
                .thenReturn(empty);

        mockMvc.perform(get("/api/v1/alerts")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        verify(service).getInbox(INSTITUTION_ID, TEACHER_USER_ID, null, null, AlertAttentionState.ALL);
    }

    @Test
    void acknowledgesAndMarksPendingOwnedAlertsWithNoRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/alerts/{alertId}/acknowledge", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/alerts/{alertId}/mark-pending", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isNoContent());

        verify(triageService).acknowledge(INSTITUTION_ID, TEACHER_USER_ID, ALERT_ID);
        verify(triageService).markPending(INSTITUTION_ID, TEACHER_USER_ID, ALERT_ID);
    }

    @Test
    void mapsUnownedAndResolvedTriageAttemptsWithoutLeakingDetails() throws Exception {
        doThrow(new AlertTriageNotFoundException())
                .when(triageService)
                .acknowledge(INSTITUTION_ID, TEACHER_USER_ID, ALERT_ID);
        doThrow(new AlertTriageConflictException())
                .when(triageService)
                .markPending(INSTITUTION_ID, TEACHER_USER_ID, ALERT_ID);

        mockMvc.perform(post("/api/v1/alerts/{alertId}/acknowledge", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/alerts/{alertId}/mark-pending", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isConflict());
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

    @Test
    void triageEndpointsRequireInstitutionAndTeacherScope() throws Exception {
        mockMvc.perform(post("/api/v1/alerts/{alertId}/acknowledge", ALERT_ID)
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/alerts/{alertId}/acknowledge", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/alerts/{alertId}/mark-pending", ALERT_ID)
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/alerts/{alertId}/mark-pending", ALERT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isBadRequest());
    }
}
