package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationStatus;
import java.time.Instant;
import java.util.UUID;

public record CommunicationResponse(
        UUID id,
        UUID alertId,
        UUID studentId,
        CommunicationStatus status,
        String subject,
        String content,
        Instant createdAt,
        Instant sentAt) {
    static CommunicationResponse from(Communication communication) {
        return new CommunicationResponse(
                communication.getId(),
                communication.getAlertId(),
                communication.getStudentId(),
                communication.getStatus(),
                communication.getSubject(),
                communication.getContent(),
                communication.getCreatedAt(),
                communication.getSentAt());
    }
}
