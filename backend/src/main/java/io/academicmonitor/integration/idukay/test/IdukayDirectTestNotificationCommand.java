package io.academicmonitor.integration.idukay.test;

import java.util.UUID;

record IdukayDirectTestNotificationCommand(UUID institutionId, UUID teacherUserId, String subject, String content) {}
