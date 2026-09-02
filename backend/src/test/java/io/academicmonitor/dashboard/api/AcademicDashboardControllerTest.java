package io.academicmonitor.dashboard.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.dashboard.application.AcademicDashboardQueryService;
import io.academicmonitor.dashboard.application.AcademicDashboardResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AcademicDashboardControllerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private AcademicDashboardQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AcademicDashboardQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AcademicDashboardController(service))
                .build();
    }

    @Test
    void returnsExpectedDashboardResponse() throws Exception {
        AcademicDashboardResponse response = new AcademicDashboardResponse(
                INSTITUTION_ID,
                TEACHER_USER_ID,
                new AcademicDashboardResponse.DashboardSummary(1, 32, 24, 18, 11, 7),
                List.of(new AcademicDashboardResponse.CourseSummary(
                        COURSE_ID, "1.º BGU A", "Física", "2025 - 2026", 32, 24, 18, 11, 7)));

        when(service.getDashboard(INSTITUTION_ID, TEACHER_USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value(INSTITUTION_ID.toString()))
                .andExpect(jsonPath("$.teacherUserId").value(TEACHER_USER_ID.toString()))
                .andExpect(jsonPath("$.summary.courses").value(1))
                .andExpect(jsonPath("$.summary.students").value(32))
                .andExpect(jsonPath("$.summary.activities").value(24))
                .andExpect(jsonPath("$.summary.openAlerts").value(18))
                .andExpect(jsonPath("$.summary.warnings").value(11))
                .andExpect(jsonPath("$.summary.critical").value(7))
                .andExpect(jsonPath("$.courses[0].id").value(COURSE_ID.toString()))
                .andExpect(jsonPath("$.courses[0].academicYear").value("2025 - 2026"));
    }

    @Test
    void requiresInstitutionId() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").queryParam("teacherUserId", TEACHER_USER_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresTeacherUserId() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isBadRequest());
    }
}
