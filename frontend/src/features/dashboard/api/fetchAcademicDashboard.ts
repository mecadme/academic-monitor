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
  institutionId: string;
  teacherUserId: string;
  signal?: AbortSignal;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080';

export async function fetchAcademicDashboard({
  institutionId,
  teacherUserId,
  signal,
}: FetchAcademicDashboardInput): Promise<AcademicDashboard> {
  const query = new URLSearchParams({
    institutionId,
    teacherUserId,
  });

  const response = await fetch(
    `${apiBaseUrl}/api/v1/dashboard?${query.toString()}`,
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
      `No se pudo cargar el dashboard académico (${response.status}).`,
    );
  }

  return response.json();
}
