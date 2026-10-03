package io.academicmonitor.integration.idukay.test;

import java.util.UUID;

public record IdukayTestNotificationResponse(UUID communicationId, String status) {}
