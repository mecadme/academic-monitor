package io.academicmonitor.academic.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.academicmonitor.academic.application.StudentGuardianQueryService;
import io.academicmonitor.academic.application.StudentGuardianQueryService.StudentGuardianResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StudentGuardianControllerTest {
    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEACHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GUARDIAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private StudentGuardianQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedAcademicContext context = mock(AuthenticatedAcademicContext.class);
        when(context.institutionId()).thenReturn(INSTITUTION_ID);
        when(context.userId()).thenReturn(TEACHER_ID);
        service = mock(StudentGuardianQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentGuardianController(service, context))
                .build();
    }

    @Test
    void exposesOnlyNeutralGuardianFields() throws Exception {
        when(service.getGuardians(STUDENT_ID, INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of(new StudentGuardianResponse(
                        GUARDIAN_ID, "Guardian One", null, true, "Parent", true, true, false, true, true)));

        mockMvc.perform(get("/api/v1/students/{studentId}/guardians", STUDENT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(GUARDIAN_ID.toString()))
                .andExpect(jsonPath("$[0].displayName").value("Guardian One"))
                .andExpect(jsonPath("$[0].email").isEmpty())
                .andExpect(jsonPath("$[0].officialLegalGuardian").value(true))
                .andExpect(jsonPath("$[0].externalId").doesNotExist())
                .andExpect(jsonPath("$[0].externalUserId").doesNotExist());
        verify(service).getGuardians(STUDENT_ID, INSTITUTION_ID, TEACHER_ID);
    }

    @Test
    void doesNotRequireTeacherAndInstitutionInputs() throws Exception {
        mockMvc.perform(get("/api/v1/students/{studentId}/guardians", STUDENT_ID)
                        .queryParam("institutionId", INSTITUTION_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/{studentId}/guardians", STUDENT_ID)
                        .queryParam("teacherUserId", TEACHER_ID.toString()))
                .andExpect(status().isOk());
    }
}
