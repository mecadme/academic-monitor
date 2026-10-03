package io.academicmonitor.notification.application;

import java.util.List;

public record AppNotificationListResponse(List<AppNotificationResponse> items, long unreadCount) {}
