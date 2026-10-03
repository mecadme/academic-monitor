package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.CommunicationStatus;
import java.util.UUID;

public record ManualNotificationResult(
        UUID communicationId, CommunicationStatus status, ManualNotificationFailure failure) {

    public static ManualNotificationResult sent(UUID communicationId) {
        return new ManualNotificationResult(communicationId, CommunicationStatus.SENT, null);
    }

    public static ManualNotificationResult failed(UUID communicationId, ManualNotificationFailure failure) {
        return new ManualNotificationResult(communicationId, CommunicationStatus.FAILED, failure);
    }

    public static ManualNotificationResult blocked(ManualNotificationFailure failure) {
        return new ManualNotificationResult(null, null, failure);
    }

    public boolean succeeded() {
        return status == CommunicationStatus.SENT;
    }
}
