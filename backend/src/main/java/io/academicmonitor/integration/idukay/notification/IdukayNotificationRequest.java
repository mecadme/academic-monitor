package io.academicmonitor.integration.idukay.notification;

import java.util.List;

public record IdukayNotificationRequest(
        List<IdukayNotificationRecipient> recipients, String subject, String content, boolean sent, String sender) {}
