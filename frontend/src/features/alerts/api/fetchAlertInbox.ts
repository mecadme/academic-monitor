export type AlertSeverity = 'CRITICAL' | 'WARNING';
export type AlertAttentionState =
  | 'PENDING'
  | 'ACKNOWLEDGED'
  | 'ALL';

export type AlertInbox = {
  institutionId: string;
  teacherUserId: string;
  total: number;
  alerts: AlertInboxItem[];
};

export type AlertInboxItem = {
  id: string;
  severity: AlertSeverity;
  ruleCode: string;
  score: number;
  acknowledgedAt: string | null;
  course: {
    id: string;
    name: string;
    subject: string | null;
  };
  activity: {
    id: string;
    name: string;
    maximumScore: number;
    dueDate: string | null;
  };
  student: {
    id: string;
    name: string;
  };
};

export type FetchAlertInboxInput = {
  institutionId: string;
  teacherUserId: string;
  courseId?: string | null;
  academicPeriodId?: string | null;
  attentionState?: AlertAttentionState;
  signal?: AbortSignal;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080';

export async function fetchAlertInbox({
  institutionId,
  teacherUserId,
  courseId,
  academicPeriodId,
  attentionState,
  signal,
}: FetchAlertInboxInput): Promise<AlertInbox> {
  const query = new URLSearchParams({
    institutionId,
    teacherUserId,
  });

  if (courseId) {
    query.set('courseId', courseId);
  }

  if (academicPeriodId) {
    query.set('academicPeriodId', academicPeriodId);
  }

  if (attentionState) {
    query.set('attentionState', attentionState);
  }

  const response = await fetch(
    `${apiBaseUrl}/api/v1/alerts?${query.toString()}`,
    {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    },
  );

  if (!response.ok) {
    throw new Error(
      `No se pudieron cargar las alertas (${response.status}).`,
    );
  }

  return response.json();
}
