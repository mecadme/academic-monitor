import { afterEach, expect, it, vi } from 'vitest';

import { fetchAcademicDashboard } from '../features/dashboard/api/fetchAcademicDashboard';
import { fetchAcademicPeriods } from '../features/alerts/api/fetchAcademicPeriods';
import { fetchAlertInbox } from '../features/alerts/api/fetchAlertInbox';
import { acknowledgeAlert, markAlertPending } from '../features/alerts/api/triageAlert';
import { fetchCommunication, fetchCommunications, prepareAlertCommunication, saveCommunicationDraft, sendCommunication, deleteCommunicationDraft } from '../features/alerts/api/communications';
import { fetchNotifications, markNotificationRead, markAllNotificationsRead } from '../features/notifications/api/notifications';
import { testIdukayLogin } from '../features/idukay/api/testIdukayLogin';
import { getIdukayPeriods } from '../features/idukay/api/getIdukayPeriods';
import { syncIdukayPeriod } from '../features/idukay/api/syncIdukayPeriod';
import { getDashboard, syncDemo } from './demo';

vi.mock('../features/idukay/lib/idukayFingerprint', () => ({ createIdukayFingerprint: async () => 'test-fingerprint' }));
afterEach(() => vi.unstubAllGlobals());

it('all academic and Idukay transports omit client ownership and share cookie/CSRF handling', async () => {
  const fetchMock = vi.fn().mockImplementation(async () => Response.json({}));
  vi.stubGlobal('fetch', fetchMock);
  await fetchAcademicDashboard({ academicPeriodId: 'period-a' });
  await fetchAcademicPeriods({});
  await fetchAlertInbox({ courseId: 'course-a', academicPeriodId: 'period-a' });
  await acknowledgeAlert({ alertId: 'alert-a' });
  await markAlertPending({ alertId: 'alert-a' });
  await fetchCommunications('DRAFT');
  await fetchCommunication('communication-a');
  await prepareAlertCommunication('alert-a');
  await saveCommunicationDraft('communication-a', 'Asunto', 'Contenido');
  await sendCommunication('communication-a');
  await deleteCommunicationDraft('communication-a');
  await fetchNotifications();
  await markNotificationRead('notification-a');
  await markAllNotificationsRead();
  await testIdukayLogin({ email: 'external@example.com', password: 'test-only-password' });
  await getIdukayPeriods();
  await syncIdukayPeriod({ periodExternalId: 'external-period-a' });
  await syncDemo('INITIAL');
  await getDashboard();

  expect(fetchMock).toHaveBeenCalledTimes(19);
  for (const [url, init] of fetchMock.mock.calls) {
    expect(url).not.toMatch(/institutionId|teacherUserId/);
    expect(init?.body ?? '').not.toMatch(/institutionId|teacherUserId/);
    expect(init?.credentials).toBe('include');
    if (['POST', 'PATCH', 'DELETE'].includes(init?.method ?? 'GET')) {
      expect(new Headers(init?.headers).get('X-XSRF-TOKEN')).toBe('test-csrf');
    }
  }
});
