package io.academicmonitor.integration.idukay.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.client.IdukayNotificationClient;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRequest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

class IdukayDirectTestNotificationServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TEST_RECIPIENT = "configured-test-recipient";

    private IdukaySessionProvider sessions;
    private IdukayNotificationClient client;
    private IdukayDirectTestNotificationService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        sessions = mock(IdukaySessionProvider.class);
        client = mock(IdukayNotificationClient.class);
        service = service(TEST_RECIPIENT);
        serviceLogger = (Logger) LoggerFactory.getLogger(IdukayDirectTestNotificationService.class);
        logs = new ListAppender<>();
        logs.start();
        serviceLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logs);
        logs.stop();
    }

    @Test
    void doesNotPostWhenTheTestRecipientIsNotConfigured() {
        IdukayDirectTestNotificationResult result = service("").send(command());

        assertEquals(IdukayDirectTestNotificationFailure.TEST_RECIPIENT_NOT_CONFIGURED, result.failure());
        verifyNoInteractions(sessions, client);
    }

    @Test
    void usesExactlyTheConfiguredRecipientAndAuthenticatedSessionUserAsSender() {
        IdukayAuthenticatedSession session = session("authenticated-id", "working-profile-id");
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID)).thenReturn(session);
        when(client.post(any(), any())).thenReturn(204);

        IdukayDirectTestNotificationResult result = service.send(command());

        ArgumentCaptor<IdukayNotificationRequest> notification =
                ArgumentCaptor.forClass(IdukayNotificationRequest.class);
        verify(client).post(eq(session), notification.capture());
        assertEquals(
                TEST_RECIPIENT, notification.getValue().recipients().getFirst().user());
        assertEquals(false, notification.getValue().recipients().getFirst().read());
        assertEquals("authenticated-id", notification.getValue().sender());
        assertEquals(true, notification.getValue().sent());
        assertEquals(true, result.sent());
        assertEquals(null, result.failure());
    }

    @Test
    void payloadDoesNotContainSelectedUsers() throws Exception {
        IdukayAuthenticatedSession session = session("authenticated-id", "working-profile-id");
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID)).thenReturn(session);
        when(client.post(any(), any())).thenReturn(200);

        service.send(command());

        ArgumentCaptor<IdukayNotificationRequest> notification =
                ArgumentCaptor.forClass(IdukayNotificationRequest.class);
        verify(client).post(eq(session), notification.capture());
        assertFalse(
                new ObjectMapper().writeValueAsString(notification.getValue()).contains("selected_users"));
    }

    @Test
    void doesNotPostWhenTheAuthenticatedSessionHasNoSender() {
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID)).thenReturn(session(null, "working-profile-id"));

        IdukayDirectTestNotificationResult result = service.send(command());

        assertEquals(IdukayDirectTestNotificationFailure.SENDER_NOT_RESOLVED, result.failure());
        verifyNoInteractions(client);
    }

    @Test
    void sendsWhenIdukayReturnsAny2xxStatus() {
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID))
                .thenReturn(session("authenticated-id", "working-profile-id"));
        when(client.post(any(), any())).thenReturn(204);

        IdukayDirectTestNotificationResult result = service.send(command());

        assertEquals(true, result.sent());
    }

    @Test
    void reportsA503FailureWithExactlyOnePostAttempt() {
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID))
                .thenReturn(session("authenticated-id", "working-profile-id"));
        when(client.post(any(), any())).thenThrow(new IdukayApiException("unavailable", 503, null));

        IdukayDirectTestNotificationResult result = service.send(command());

        assertEquals(IdukayDirectTestNotificationFailure.PROVIDER_UNAVAILABLE, result.failure());
        verify(client, times(1)).post(any(), any());
    }

    @Test
    void doesNotLogRecipientSenderOrContent() {
        String sensitiveRecipient = "test-recipient-must-not-be-logged";
        String sensitiveSender = "sender-must-not-be-logged";
        String sensitiveContent = "content-must-not-be-logged";
        IdukayAuthenticatedSession session = session(sensitiveSender, "working-profile-id");
        when(sessions.getSoleSessionForInstitution(INSTITUTION_ID)).thenReturn(session);
        when(client.post(any(), any())).thenThrow(new IdukayApiException("unavailable", 503, null));

        service(sensitiveRecipient)
                .send(new IdukayDirectTestNotificationCommand(INSTITUTION_ID, "Subject", sensitiveContent));

        assertFalse(
                logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains(sensitiveRecipient)));
        assertFalse(
                logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains(sensitiveSender)));
        assertFalse(
                logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains(sensitiveContent)));
    }

    private IdukayDirectTestNotificationService service(String recipient) {
        return new IdukayDirectTestNotificationService(sessions, client, recipient);
    }

    private static IdukayDirectTestNotificationCommand command() {
        return new IdukayDirectTestNotificationCommand(INSTITUTION_ID, "Subject", "<p>Content</p>");
    }

    private static IdukayAuthenticatedSession session(String authenticatedUserId, String workingProfile) {
        return IdukayAuthenticatedSession.create(
                "synthetic-token",
                authenticatedUserId,
                new IdukaySessionContext(
                        "year-id", "school-id", null, null, workingProfile, "staff", "-05:00", "permissions"),
                RestClient.create());
    }
}
