package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.client.IdukayNotificationClient;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRecipient;
import io.academicmonitor.integration.idukay.notification.IdukayNotificationRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Development-only transport validation. It deliberately does not create a Communication or
 * inspect academic scope and StudentGuardian relationships.
 */
@Service
public class IdukayDirectTestNotificationService {

    private final IdukaySessionProvider sessionProvider;
    private final IdukayNotificationClient notificationClient;
    private final String testNotificationRecipientUserId;

    public IdukayDirectTestNotificationService(
            IdukaySessionProvider sessionProvider,
            IdukayNotificationClient notificationClient,
            @Value("${app.idukay.test-notification-recipient-user-id:}") String testNotificationRecipientUserId) {
        this.sessionProvider = sessionProvider;
        this.notificationClient = notificationClient;
        this.testNotificationRecipientUserId = testNotificationRecipientUserId;
    }

    public IdukayDirectTestNotificationResult send(IdukayDirectTestNotificationCommand command) {
        if (command == null || command.institutionId() == null) {
            throw new IllegalArgumentException("direct notification scope is required");
        }

        if (testNotificationRecipientUserId == null || testNotificationRecipientUserId.isBlank()) {
            return IdukayDirectTestNotificationResult.failed(
                    IdukayDirectTestNotificationFailure.TEST_RECIPIENT_NOT_CONFIGURED);
        }

        IdukayAuthenticatedSession session;
        try {
            session = sessionProvider.getSoleSessionForInstitution(command.institutionId());
        } catch (RuntimeException exception) {
            return IdukayDirectTestNotificationResult.failed(IdukayDirectTestNotificationFailure.PROVIDER_UNAVAILABLE);
        }
        String sender = session.authenticatedUserId();
        if (sender == null || sender.isBlank()) {
            return IdukayDirectTestNotificationResult.failed(IdukayDirectTestNotificationFailure.SENDER_NOT_RESOLVED);
        }

        IdukayNotificationRequest notification = new IdukayNotificationRequest(
                List.of(new IdukayNotificationRecipient(testNotificationRecipientUserId, false)),
                command.subject(),
                command.content(),
                true,
                sender);
        try {
            int status = notificationClient.post(session, notification);
            return status >= 200 && status < 300
                    ? IdukayDirectTestNotificationResult.successful()
                    : IdukayDirectTestNotificationResult.failed(
                            IdukayDirectTestNotificationFailure.PROVIDER_UNAVAILABLE);
        } catch (IdukayApiException exception) {
            Integer status = exception.getStatusCode();
            return IdukayDirectTestNotificationResult.failed(
                    status != null && status >= 400 && status < 500
                            ? IdukayDirectTestNotificationFailure.PROVIDER_REJECTED
                            : IdukayDirectTestNotificationFailure.PROVIDER_UNAVAILABLE);
        } catch (RuntimeException exception) {
            return IdukayDirectTestNotificationResult.failed(IdukayDirectTestNotificationFailure.PROVIDER_UNAVAILABLE);
        }
    }
}
