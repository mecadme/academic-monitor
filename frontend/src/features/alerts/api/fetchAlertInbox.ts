import { apiFetch } from '../../../api/apiFetch';

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
  communication: {
    id: string;
    status: 'DRAFT' | 'PENDING' | 'SENT' | 'FAILED';
  } | null;
};

export type FetchAlertInboxInput = {
  courseId?: string | null;
  academicPeriodId?: string | null;
  attentionState?: AlertAttentionState;
  signal?: AbortSignal;
};


export async function fetchAlertInbox({
  courseId,
  academicPeriodId,
  attentionState,
  signal,
}: FetchAlertInboxInput): Promise<AlertInbox> {
  const query = new URLSearchParams();

  if (courseId) {
    query.set('courseId', courseId);
  }

  if (academicPeriodId) {
    query.set('academicPeriodId', academicPeriodId);
  }

  if (attentionState) {
    query.set('attentionState', attentionState);
  }

  const response = await apiFetch(
    `/api/v1/alerts${query.size ? `?${query.toString()}` : ''}`,
    {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    },
    { errorMessage: (status) => `No se pudieron cargar las alertas (${status}).` },
  );


  return response.json();
}
