package io.academicmonitor.integration.idukay.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.communication.application.ManualNotificationResult;
import io.academicmonitor.communication.application.ManualNotificationService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdukayTestNotificationControllerTest {

    private ManualNotificationService service;
    private IdukayDirectTestNotificationService directService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ManualNotificationService.class);
        directService = mock(IdukayDirectTestNotificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IdukayTestNotificationController(service, directService))
                .build();
    }

    @Test
    void exposesOnlyTheInternalCommunicationIdAndStatus() throws Exception {
        UUID communicationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(service.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ManualNotificationResult.sent(communicationId));

        mockMvc.perform(
                        post("/api/v1/integrations/idukay/test-notification")
                                .contentType("application/json")
                                .content(
                                        """
                                {
                                  "institutionId":"22222222-2222-2222-2222-222222222222",
                                  "teacherUserId":"33333333-3333-3333-3333-333333333333",
                                  "studentId":"44444444-4444-4444-4444-444444444444",
                                  "subject":"Subject",
                                  "content":"<p>Content</p>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communicationId").value(communicationId.toString()))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sender").doesNotExist())
                .andExpect(jsonPath("$.externalUserId").doesNotExist())
                .andExpect(jsonPath("$.parentProfileId").doesNotExist());
    }

    @Test
    void directResponseDoesNotExposeIdukayIdentifiers() throws Exception {
        when(directService.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(IdukayDirectTestNotificationResult.successful());

        mockMvc.perform(
                        post("/api/v1/integrations/idukay/test-notification-direct")
                                .contentType("application/json")
                                .content(
                                        """
                                {
                                  "institutionId":"22222222-2222-2222-2222-222222222222",
                                  "subject":"Subject",
                                  "content":"<p>Content</p>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sender").doesNotExist())
                .andExpect(jsonPath("$.recipient").doesNotExist())
                .andExpect(jsonPath("$.externalUserId").doesNotExist())
                .andExpect(jsonPath("$.parentProfileId").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "guardianId",
                "recipient",
                "externalUserId",
                "parentProfileId",
                "sender",
                "workingprofile",
                "teacherUserId",
                "teacherExternalId"
            })
    void directRequestRejectsEveryClientControlledTransportIdentifier(String unexpectedField) throws Exception {
        mockMvc.perform(post("/api/v1/integrations/idukay/test-notification-direct")
                        .contentType("application/json")
                        .content(String.format(
                                """
                                {
                                  "institutionId":"22222222-2222-2222-2222-222222222222",
                                  "subject":"Subject",
                                  "content":"<p>Content</p>",
                                  "%s":"attacker-controlled"
                                }
                                """,
                                unexpectedField)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(directService);
    }
}
