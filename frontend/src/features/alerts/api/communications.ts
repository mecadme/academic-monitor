import { apiFetch } from '../../../api/apiFetch';

export type Communication = {
  id: string;
  alertId: string;
  studentId: string;
  status: 'DRAFT' | 'PENDING' | 'SENT' | 'FAILED';
  subject: string;
  content: string;
  createdAt: string;
  sentAt: string | null;
  studentName: string | null;
  courseName: string | null;
  courseSubject: string | null;
  activityName: string | null;
  score: number | null;
  maximumScore: number | null;
  alertSeverity: 'CRITICAL' | 'WARNING' | null;
};

export const communicationsRefreshEvent = 'academic-monitor:communications-refresh';

export async function fetchCommunications(status?: Communication['status']): Promise<Communication[]> {
  const query = new URLSearchParams();
  if (status) query.set('status', status);
  return (await apiFetch(`/api/v1/communications${query.size ? `?${query}` : ''}`, {}, {
    errorMessage: 'No se pudieron cargar las comunicaciones.',
  })).json();
}

export async function fetchCommunication(communicationId: string): Promise<Communication> {
  return request(`/api/v1/communications/${encodeURIComponent(communicationId)}`, { method: 'GET' });
}

export async function deleteCommunicationDraft(communicationId: string): Promise<void> {
  await apiFetch(`/api/v1/communications/${encodeURIComponent(communicationId)}`, { method: 'DELETE' }, {
    errorMessage: (status) => status === 409 ? 'El borrador cambió de estado y ya no se puede eliminar.' : 'No se pudo eliminar el borrador.',
  });
  window.dispatchEvent(new Event(communicationsRefreshEvent));
}

export async function prepareAlertCommunication(alertId: string): Promise<Communication> {
  return request(`/api/v1/alerts/${encodeURIComponent(alertId)}/communication`, { method: 'POST' });
}

export async function saveCommunicationDraft(communicationId: string, subject: string, content: string): Promise<Communication> {
  return request(`/api/v1/communications/${encodeURIComponent(communicationId)}`, {
    method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ subject, content }),
  });
}

export async function sendCommunication(communicationId: string): Promise<Communication> {
  try {
    return await request(`/api/v1/communications/${encodeURIComponent(communicationId)}/send`, { method: 'POST' });
  } finally {
    window.dispatchEvent(new Event('academic-monitor:notifications-refresh'));
  }
}

async function request(path: string, init: RequestInit): Promise<Communication> {
  return (await apiFetch(path, init, { errorMessage: 'No se pudo completar la comunicación.' })).json();
}
