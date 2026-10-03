const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export type AppNotificationType = 'NEW_CRITICAL_ALERT' | 'COMMUNICATION_SENT' | 'COMMUNICATION_FAILED' | 'SYNC_COMPLETED' | 'SYNC_FAILED';
export type AppNotificationReferenceType = 'ALERT' | 'COMMUNICATION' | 'SYNC' | 'NONE';
export type AppNotification = {
  id: string;
  type: AppNotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  reference: { type: AppNotificationReferenceType; id: string | null };
};
export type AppNotificationList = { items: AppNotification[]; unreadCount: number };
export type NotificationScope = { institutionId: string; teacherUserId: string };

export async function fetchNotifications(scope: NotificationScope, unreadOnly = false, limit = 20): Promise<AppNotificationList> {
  const query = new URLSearchParams({ ...scope, unreadOnly: String(unreadOnly), limit: String(limit) });
  const response = await fetch(`${apiBaseUrl}/api/v1/notifications?${query.toString()}`, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('No se pudieron cargar las notificaciones.');
  return response.json() as Promise<AppNotificationList>;
}

export async function markNotificationRead(id: string, scope: NotificationScope): Promise<AppNotification> {
  const response = await fetch(`${apiBaseUrl}/api/v1/notifications/${encodeURIComponent(id)}/read?${new URLSearchParams(scope)}`, { method: 'POST', headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('No se pudo actualizar la notificación.');
  return response.json() as Promise<AppNotification>;
}

export async function markAllNotificationsRead(scope: NotificationScope): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/v1/notifications/read-all?${new URLSearchParams(scope)}`, { method: 'POST' });
  if (!response.ok) throw new Error('No se pudieron marcar las notificaciones.');
}
