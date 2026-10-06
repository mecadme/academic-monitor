package io.academicmonitor.integration.idukay.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.communication.application.ManualNotificationRequest;
import io.academicmonitor.communication.application.ManualNotificationResult;
import io.academicmonitor.communication.application.ManualNotificationService;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdukayTestNotificationControllerTest {

    private ManualNotificationService service;
    private IdukayDirectTestNotificationService directService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedAcademicContext context = mock(AuthenticatedAcademicContext.class);
        when(context.institutionId()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(context.userId()).thenReturn(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        service = mock(ManualNotificationService.class);
        directService = mock(IdukayDirectTestNotificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IdukayTestNotificationController(service, directService, context))
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
                                  "institutionId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "teacherUserId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
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
        var captured = ArgumentCaptor.forClass(ManualNotificationRequest.class);
        verify(service).send(captured.capture());
        assertEquals(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                captured.getValue().institutionId());
        assertEquals(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                captured.getValue().teacherUserId());
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
        var captured = ArgumentCaptor.forClass(IdukayDirectTestNotificationCommand.class);
        verify(directService).send(captured.capture());
        assertEquals(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                captured.getValue().institutionId());
        assertEquals(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                captured.getValue().teacherUserId());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "institutionId",
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
