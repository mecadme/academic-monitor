package io.academicmonitor.academic.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.academic.application.AcademicPeriodCatalogQueryService;
import io.academicmonitor.academic.application.AcademicPeriodCatalogResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AcademicPeriodControllerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIOD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private AcademicPeriodCatalogQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedAcademicContext context = mock(AuthenticatedAcademicContext.class);
        when(context.institutionId()).thenReturn(INSTITUTION_ID);
        when(context.userId()).thenReturn(TEACHER_ID);
        service = mock(AcademicPeriodCatalogQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AcademicPeriodController(service, context))
                .build();
    }

    @Test
    void returnsNeutralInternalPeriodMetadata() throws Exception {
        AcademicPeriodCatalogResponse response = new AcademicPeriodCatalogResponse(
                INSTITUTION_ID,
                TEACHER_ID,
                List.of(new AcademicPeriodCatalogResponse.AcademicPeriodItem(
                        PERIOD_ID,
                        ACADEMIC_YEAR_ID,
                        "period-external-id",
                        "Primer trimestre",
                        "T1",
                        1,
                        "2025 - 2026",
                        true)));
        when(service.getPeriods(INSTITUTION_ID, TEACHER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/academic-periods")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value(INSTITUTION_ID.toString()))
                .andExpect(jsonPath("$.teacherUserId").value(TEACHER_ID.toString()))
                .andExpect(jsonPath("$.periods[0].id").value(PERIOD_ID.toString()))
                .andExpect(jsonPath("$.periods[0].academicYearId").value(ACADEMIC_YEAR_ID.toString()))
                .andExpect(jsonPath("$.periods[0].externalId").value("period-external-id"))
                .andExpect(jsonPath("$.periods[0].name").value("Primer trimestre"))
                .andExpect(jsonPath("$.periods[0].abbreviation").value("T1"))
                .andExpect(jsonPath("$.periods[0].order").value(1))
                .andExpect(jsonPath("$.periods[0].academicYear").value("2025 - 2026"))
                .andExpect(jsonPath("$.periods[0].synchronized").value(true))
                .andExpect(jsonPath("$.periods[0].platformCode").doesNotExist());

        verify(service).getPeriods(INSTITUTION_ID, TEACHER_ID);
    }

    @Test
    void doesNotRequireClientScopeParameters() throws Exception {
        mockMvc.perform(get("/api/v1/academic-periods").queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/academic-periods").queryParam("teacherUserId", TEACHER_ID.toString()))
                .andExpect(status().isOk());
    }
}
