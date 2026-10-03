package io.academicmonitor.integration.idukay.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.academicmonitor.communication.application.port.CommunicationDeliveryRequest;
import io.academicmonitor.communication.application.port.DeliveryResult;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayNotificationClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

class IdukayCommunicationAdapterTest {

    @Test
    void senderComesFromAuthenticatedUserIdNotWorkingProfileOrRequest() {
        IdukaySessionProvider sessions = mock(IdukaySessionProvider.class);
        IdukayNotificationClient client = mock(IdukayNotificationClient.class);
        IdukayAuthenticatedSession session = session("authenticated-id", "working-profile-id");
        when(sessions.getSession(any())).thenReturn(session);
        when(client.post(any(), any())).thenReturn(200);
        IdukayCommunicationAdapter adapter = new IdukayCommunicationAdapter(sessions, client);

        DeliveryResult result = adapter.send(request());

        ArgumentCaptor<IdukayNotificationRequest> notification =
                ArgumentCaptor.forClass(IdukayNotificationRequest.class);
        verify(client).post(eq(session), notification.capture());
        assertEquals("authenticated-id", notification.getValue().sender());
        assertEquals(
                "guardian-user-id",
                notification.getValue().recipients().getFirst().user());
        assertEquals(false, notification.getValue().recipients().getFirst().read());
        assertEquals(true, notification.getValue().sent());
        assertEquals("Subject", notification.getValue().subject());
        assertEquals("<p>Content</p>", notification.getValue().content());
        assertEquals(true, result.delivered());
    }

    @Test
    void senderNotResolvedReturnsControlledResultWithoutAPost() {
        IdukaySessionProvider sessions = mock(IdukaySessionProvider.class);
        IdukayNotificationClient client = mock(IdukayNotificationClient.class);
        when(sessions.getSession(any())).thenReturn(session(null, "working-profile-id"));
        IdukayCommunicationAdapter adapter = new IdukayCommunicationAdapter(sessions, client);

        DeliveryResult result = adapter.send(request());

        assertEquals(false, result.delivered());
        assertEquals("SENDER_NOT_RESOLVED", result.failureCode());
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 503})
    void mapsProviderHttpFailuresWithoutRetrying(int status) {
        IdukaySessionProvider sessions = mock(IdukaySessionProvider.class);
        IdukayNotificationClient client = mock(IdukayNotificationClient.class);
        IdukayAuthenticatedSession session = session("authenticated-id", "working-profile-id");
        when(sessions.getSession(any())).thenReturn(session);
        when(client.post(any(), any()))
                .thenThrow(
                        new io.academicmonitor.integration.idukay.client.IdukayApiException("failure", status, null));
        IdukayCommunicationAdapter adapter = new IdukayCommunicationAdapter(sessions, client);

        DeliveryResult result = adapter.send(request());

        assertEquals(status < 500 ? "PROVIDER_REJECTED" : "PROVIDER_UNAVAILABLE", result.failureCode());
        verify(client, times(1)).post(any(), any());
    }

    private static CommunicationDeliveryRequest request() {
        return new CommunicationDeliveryRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "guardian-user-id",
                "Subject",
                "<p>Content</p>");
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
