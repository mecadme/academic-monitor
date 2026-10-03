package io.academicmonitor.integration.idukay.test;

import java.util.UUID;

record IdukayDirectTestNotificationCommand(UUID institutionId, String subject, String content) {}
