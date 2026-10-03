package io.academicmonitor.communication.application;

public enum ManualNotificationFailure {
    RECIPIENT_NOT_FOUND,
    RECIPIENT_AMBIGUOUS,
    RECIPIENT_NOT_REACHABLE,
    SENDER_NOT_RESOLVED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_REJECTED,
    STUDENT_OUT_OF_SCOPE
}
