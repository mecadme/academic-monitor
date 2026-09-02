package io.academicmonitor.context.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.context.application.AcademicContextBootstrapService;
import io.academicmonitor.context.application.AcademicContextResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AcademicContextControllerTest {

    @Test
    void returnsInstitutionAndTeacherUserIds() throws Exception {
        UUID institutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID teacherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AcademicContextBootstrapService service = org.mockito.Mockito.mock(AcademicContextBootstrapService.class);
        org.mockito.Mockito.when(service.bootstrap())
                .thenReturn(new AcademicContextResult(institutionId, teacherUserId));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AcademicContextController(service))
                .build();

        mockMvc.perform(post("/api/v1/context/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value(institutionId.toString()))
                .andExpect(jsonPath("$.teacherUserId").value(teacherUserId.toString()));
    }
}
