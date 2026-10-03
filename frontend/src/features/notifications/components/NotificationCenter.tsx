import { AlertTriangle, Bell, Check, CircleAlert, RefreshCw, X, type LucideIcon } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';

import { type AppNotification, type AppNotificationType, type NotificationScope } from '../api/notifications';
import { useNotifications } from '../hooks/useNotifications';

type Navigation = (path: string) => void;
type Props = { scope: NotificationScope; onNavigate: Navigation };

export function NotificationCenter({ scope, onNavigate }: Props) {
  const [open, setOpen] = useState(false);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const notifications = useNotifications(scope);
  const unreadLabel = notifications.unreadCount > 99 ? '99+' : String(notifications.unreadCount);
  const visible = unreadOnly ? notifications.items.filter((item) => !item.read) : notifications.items;

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false); };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, []);

  const openNotification = async (item: AppNotification) => {
    try { await notifications.markRead(item); } catch { /* Navigation should remain useful if only read state failed. */ }
    setOpen(false);
    onNavigate(notificationPath(item));
  };

  return <div className="notification-center" ref={panelRef}>
    <button className="notification-button" type="button" aria-label={`Notificaciones, ${notifications.unreadCount} sin leer`} aria-expanded={open} aria-haspopup="dialog" onClick={() => setOpen((value) => !value)}>
      <Bell size={19} />
      {notifications.unreadCount > 0 && <span className="notification-badge" aria-hidden="true">{unreadLabel}</span>}
    </button>
    {open && <section className="notification-popover" role="dialog" aria-label="Notificaciones">
      <header className="notification-popover-header"><h2>Notificaciones</h2><button className="text-button" type="button" disabled={notifications.unreadCount === 0} onClick={() => void notifications.markAllRead()}>Marcar todas como leídas</button></header>
      <div className="notification-tabs" role="tablist" aria-label="Filtrar notificaciones"><button type="button" role="tab" aria-selected={!unreadOnly} className={!unreadOnly ? 'is-active' : ''} onClick={() => setUnreadOnly(false)}>Todas</button><button type="button" role="tab" aria-selected={unreadOnly} className={unreadOnly ? 'is-active' : ''} onClick={() => setUnreadOnly(true)}>No leídas</button></div>
      <NotificationContent items={visible} loading={notifications.loading} error={notifications.error} onRetry={notifications.refresh} onOpen={openNotification} compact />
      <footer className="notification-popover-footer"><button className="text-button" type="button" onClick={() => { setOpen(false); onNavigate('/notifications'); }}>Ver todas</button></footer>
    </section>}
  </div>;
}

export function NotificationsPage({ scope, onNavigate }: Props) {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const notifications = useNotifications(scope, 50);
  const items = unreadOnly ? notifications.items.filter((item) => !item.read) : notifications.items;
  const openNotification = async (item: AppNotification) => {
    try { await notifications.markRead(item); } catch { /* Navigation does not depend on read persistence. */ }
    onNavigate(notificationPath(item));
  };
  return <section className="page notifications-page"><header className="page-title"><h1>Notificaciones</h1><p>Eventos recientes de Academic Monitor.</p></header><div className="notifications-page-actions"><div className="tabs notification-page-tabs" role="tablist"><button type="button" role="tab" aria-selected={!unreadOnly} className={!unreadOnly ? 'is-active' : ''} onClick={() => setUnreadOnly(false)}>Todas</button><button type="button" role="tab" aria-selected={unreadOnly} className={unreadOnly ? 'is-active' : ''} onClick={() => setUnreadOnly(true)}>No leídas</button></div><button className="text-button" type="button" disabled={notifications.unreadCount === 0} onClick={() => void notifications.markAllRead()}>Marcar todas como leídas</button></div><NotificationContent items={items} loading={notifications.loading} error={notifications.error} onRetry={notifications.refresh} onOpen={openNotification} /></section>;
}

type ContentProps = { items: AppNotification[]; loading: boolean; error: string | null; onRetry: () => Promise<void>; onOpen: (item: AppNotification) => void; compact?: boolean };
function NotificationContent({ items, loading, error, onRetry, onOpen, compact = false }: ContentProps) {
  if (loading) return <p className="notification-feedback" role="status">Cargando notificaciones…</p>;
  if (error) return <div className="notification-feedback" role="alert"><p>{error}</p><button className="text-button" type="button" onClick={() => void onRetry()}>Reintentar</button></div>;
  if (items.length === 0) return <div className="notification-empty"><Bell size={22} aria-hidden="true" /><p>No tienes notificaciones nuevas.</p></div>;
  return <div className={`notification-list ${compact ? 'is-compact' : ''}`}>{items.map((item) => <NotificationItem key={item.id} item={item} onOpen={onOpen} />)}</div>;
}

function NotificationItem({ item, onOpen }: { item: AppNotification; onOpen: (item: AppNotification) => void }) {
  const Icon = iconFor(item.type);
  return <button className={`notification-item ${item.read ? 'is-read' : 'is-unread'}`} type="button" onClick={() => onOpen(item)}><span className="notification-icon" aria-hidden="true"><Icon size={18} /></span><span className="notification-copy"><strong>{item.title}</strong><span>{item.message}</span><time dateTime={item.createdAt}>{relativeTime(item.createdAt)}</time></span>{!item.read && <span className="notification-unread-indicator"><span className="sr-only">No leída</span></span>}</button>;
}

function iconFor(type: AppNotificationType): LucideIcon { return ({ NEW_CRITICAL_ALERT: AlertTriangle, COMMUNICATION_SENT: Check, COMMUNICATION_FAILED: CircleAlert, SYNC_COMPLETED: RefreshCw, SYNC_FAILED: X })[type]; }
function notificationPath(item: AppNotification) { if (item.reference.type === 'COMMUNICATION' && item.reference.id) return `/communications/${item.reference.id}`; if (item.reference.type === 'ALERT') return `/alerts${item.reference.id ? `?alertId=${item.reference.id}` : ''}`; return '/'; }
function relativeTime(value: string) { const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60000)); if (minutes < 1) return 'Ahora'; if (minutes < 60) return `Hace ${minutes} min`; const hours = Math.round(minutes / 60); if (hours < 24) return `Hace ${hours} h`; return new Intl.DateTimeFormat('es-EC', { dateStyle: 'medium' }).format(new Date(value)); }
