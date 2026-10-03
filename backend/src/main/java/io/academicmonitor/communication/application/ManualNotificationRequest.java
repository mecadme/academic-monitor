package io.academicmonitor.communication.application;

import java.util.UUID;

public record ManualNotificationRequest(
        UUID institutionId, UUID teacherUserId, UUID studentId, String subject, String content) {}
