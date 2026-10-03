import { useCallback, useEffect, useState } from 'react';

import { fetchNotifications, markAllNotificationsRead, markNotificationRead, type AppNotification, type AppNotificationList, type NotificationScope } from '../api/notifications';

export function useNotifications(scope: NotificationScope, limit = 20) {
  const [result, setResult] = useState<AppNotificationList | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try { setResult(await fetchNotifications(scope, false, limit)); }
    catch (err) { setError(err instanceof Error ? err.message : 'No se pudieron cargar las notificaciones.'); }
    finally { setLoading(false); }
  }, [limit, scope.institutionId, scope.teacherUserId]);

  useEffect(() => { void refresh(); }, [refresh]);
  useEffect(() => {
    const onFocus = () => { void refresh(); };
    const onRefresh = () => { void refresh(); };
    window.addEventListener('focus', onFocus);
    window.addEventListener('academic-monitor:notifications-refresh', onRefresh);
    return () => {
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('academic-monitor:notifications-refresh', onRefresh);
    };
  }, [refresh]);

  const markRead = useCallback(async (notification: AppNotification) => {
    if (notification.read) return notification;
    const updated = await markNotificationRead(notification.id, scope);
    setResult((current) => current && { unreadCount: Math.max(0, current.unreadCount - 1), items: current.items.map((item) => item.id === updated.id ? updated : item) });
    return updated;
  }, [scope.institutionId, scope.teacherUserId]);

  const markAllRead = useCallback(async () => {
    await markAllNotificationsRead(scope);
    setResult((current) => current && { unreadCount: 0, items: current.items.map((item) => ({ ...item, read: true })) });
  }, [scope.institutionId, scope.teacherUserId]);

  return { items: result?.items ?? [], unreadCount: result?.unreadCount ?? 0, loading, error, refresh, markRead, markAllRead };
}
