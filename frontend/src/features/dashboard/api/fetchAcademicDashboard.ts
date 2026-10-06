import { apiFetch } from '../../../api/apiFetch';

export type AcademicDashboard = {
  institutionId: string;
  teacherUserId: string;
  summary: {
    courses: number;
    students: number;
    activities: number;
    openAlerts: number;
    warnings: number;
    critical: number;
  };
  courses: AcademicDashboardCourse[];
};

export type AcademicDashboardCourse = {
  id: string;
  name: string;
  subject: string | null;
  academicYear: string | null;
  students: number;
  activities: number;
  openAlerts: number;
  warnings: number;
  critical: number;
};

export type FetchAcademicDashboardInput = {
  academicPeriodId?: string | null;
  signal?: AbortSignal;
};


export async function fetchAcademicDashboard({
  academicPeriodId,
  signal,
}: FetchAcademicDashboardInput): Promise<AcademicDashboard> {
  const query = new URLSearchParams();
  if (academicPeriodId) query.set('academicPeriodId', academicPeriodId);

  const response = await apiFetch(
    `/api/v1/dashboard${query.size ? `?${query.toString()}` : ''}`,
    {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    },
    { errorMessage: (status) => `No se pudo cargar el dashboard académico (${status}).` },
  );


  return response.json();
}
