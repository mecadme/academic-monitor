package io.academicmonitor.communication.application.port;

import java.util.UUID;

public record CommunicationDeliveryRequest(
        UUID communicationId,
        UUID institutionId,
        UUID teacherUserId,
        String recipientExternalUserId,
        String subject,
        String content) {

    public CommunicationDeliveryRequest {
        if (communicationId == null || institutionId == null || teacherUserId == null) {
            throw new IllegalArgumentException("communication scope is required");
        }
        recipientExternalUserId = requireText(recipientExternalUserId, "recipientExternalUserId");
        subject = requireText(subject, "subject");
        content = requireText(content, "content");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
