import { apiFetch } from '../../../api/apiFetch';

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

export async function fetchNotifications(unreadOnly = false, limit = 20): Promise<AppNotificationList> {
  const query = new URLSearchParams({ unreadOnly: String(unreadOnly), limit: String(limit) });
  return (await apiFetch(`/api/v1/notifications?${query}`, {}, { errorMessage: 'No se pudieron cargar las notificaciones.' })).json();
}

export async function markNotificationRead(id: string): Promise<AppNotification> {
  return (await apiFetch(`/api/v1/notifications/${encodeURIComponent(id)}/read`, { method: 'POST' }, {
    errorMessage: 'No se pudo actualizar la notificación.',
  })).json();
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiFetch('/api/v1/notifications/read-all', { method: 'POST' }, { errorMessage: 'No se pudieron marcar las notificaciones.' });
}
