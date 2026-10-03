package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationStatus;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import java.math.BigDecimal;
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
        Instant sentAt,
        String studentName,
        String courseName,
        String courseSubject,
        String activityName,
        BigDecimal score,
        BigDecimal maximumScore,
        AlertSeverity alertSeverity) {
    static CommunicationResponse from(Communication communication) {
        return from(communication, null, null, null, null, null, null, null);
    }

    static CommunicationResponse from(
            Communication communication,
            String studentName,
            String courseName,
            String courseSubject,
            String activityName,
            BigDecimal score,
            BigDecimal maximumScore,
            AlertSeverity alertSeverity) {
        return new CommunicationResponse(
                communication.getId(),
                communication.getAlertId(),
                communication.getStudentId(),
                communication.getStatus(),
                communication.getSubject(),
                communication.getContent(),
                communication.getCreatedAt(),
                communication.getSentAt(),
                studentName,
                courseName,
                courseSubject,
                activityName,
                score,
                maximumScore,
                alertSeverity);
    }
}
