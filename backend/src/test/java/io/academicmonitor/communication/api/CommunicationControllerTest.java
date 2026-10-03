package io.academicmonitor.communication.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.CommunicationResponse;
import io.academicmonitor.communication.domain.CommunicationStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommunicationControllerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private AlertCommunicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AlertCommunicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CommunicationController(service))
                .build();
    }

    @Test
    void listsOnlyTheRequestedScopedStatus() throws Exception {
        when(service.list(INSTITUTION_ID, TEACHER_ID, CommunicationStatus.SENT))
                .thenReturn(List.<CommunicationResponse>of());

        mockMvc.perform(get("/api/v1/communications")
                        .queryParam("institutionId", INSTITUTION_ID.toString())
                        .queryParam("teacherUserId", TEACHER_ID.toString())
                        .queryParam("status", "SENT"))
                .andExpect(status().isOk());

        verify(service).list(INSTITUTION_ID, TEACHER_ID, CommunicationStatus.SENT);
    }
}
