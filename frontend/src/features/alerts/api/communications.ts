export type Communication = {
  id: string;
  alertId: string;
  studentId: string;
  status: 'DRAFT' | 'PENDING' | 'SENT' | 'FAILED';
  subject: string;
  content: string;
  createdAt: string;
  sentAt: string | null;
};

type Scope = {
  institutionId: string;
  teacherUserId: string;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function prepareAlertCommunication(
  alertId: string,
  scope: Scope,
): Promise<Communication> {
  return request(
    `/api/v1/alerts/${encodeURIComponent(alertId)}/communication`,
    scope,
    { method: 'POST' },
  );
}

export async function saveCommunicationDraft(
  communicationId: string,
  scope: Scope,
  subject: string,
  content: string,
): Promise<Communication> {
  return request(
    `/api/v1/communications/${encodeURIComponent(communicationId)}`,
    scope,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ subject, content }),
    },
  );
}

export async function sendCommunication(
  communicationId: string,
  scope: Scope,
): Promise<Communication> {
  return request(
    `/api/v1/communications/${encodeURIComponent(communicationId)}/send`,
    scope,
    { method: 'POST' },
  );
}

async function request(
  path: string,
  { institutionId, teacherUserId }: Scope,
  init: RequestInit,
): Promise<Communication> {
  const query = new URLSearchParams({ institutionId, teacherUserId });
  const response = await fetch(`${apiBaseUrl}${path}?${query.toString()}`, {
    ...init,
    headers: { Accept: 'application/json', ...init.headers },
  });

  if (!response.ok) {
    throw new Error('No se pudo completar la comunicación.');
  }
  return response.json() as Promise<Communication>;
}
