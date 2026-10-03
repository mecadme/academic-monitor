package io.academicmonitor.integration.idukay.notification;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.application.port.CommunicationDeliveryRequest;
import io.academicmonitor.communication.application.port.DeliveryResult;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.client.IdukayNotificationClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdukayCommunicationAdapter implements CommunicationDeliveryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdukayCommunicationAdapter.class);

    private final IdukaySessionProvider sessionProvider;
    private final IdukayNotificationClient notificationClient;

    public IdukayCommunicationAdapter(
            IdukaySessionProvider sessionProvider, IdukayNotificationClient notificationClient) {
        this.sessionProvider = sessionProvider;
        this.notificationClient = notificationClient;
    }

    @Override
    public String providerCode() {
        return "IDUKAY";
    }

    @Override
    public DeliveryResult send(CommunicationDeliveryRequest request) {
        IdukayAuthenticatedSession session;
        try {
            session = sessionProvider.getSession(
                    new AcademicPlatformContext(request.institutionId(), request.teacherUserId()));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Idukay notification session unavailable; communicationId={}, attempt=1",
                    request.communicationId());
            return DeliveryResult.failed("PROVIDER_UNAVAILABLE", "Authenticated Idukay session is unavailable");
        }

        String sender = session.authenticatedUserId();
        if (sender == null || sender.isBlank()) {
            LOGGER.warn(
                    "Idukay notification sender is unresolved; communicationId={}, attempt=1",
                    request.communicationId());
            return DeliveryResult.failed("SENDER_NOT_RESOLVED", "Authenticated Idukay user id is unavailable");
        }

        IdukayNotificationRequest notification = new IdukayNotificationRequest(
                List.of(new IdukayNotificationRecipient(request.recipientExternalUserId(), false)),
                request.subject(),
                request.content(),
                true,
                sender);
        try {
            int status = notificationClient.post(session, notification);
            LOGGER.info(
                    "Idukay notification delivered; communicationId={}, status={}, attempt=1",
                    request.communicationId(),
                    status);
            return DeliveryResult.sent();
        } catch (IdukayApiException exception) {
            Integer status = exception.getStatusCode();
            String category =
                    status != null && status >= 400 && status < 500 ? "PROVIDER_REJECTED" : "PROVIDER_UNAVAILABLE";
            LOGGER.warn(
                    "Idukay notification delivery failed; communicationId={}, status={}, attempt=1, category={}",
                    request.communicationId(),
                    status,
                    category);
            return DeliveryResult.failed(
                    category, status == null ? "Idukay communication failed" : "Idukay returned HTTP " + status);
        }
    }
}
