export type AcademicPeriod = {
  id: string;
  name: string;
  abbreviation: string | null;
  order: number;
  synchronized: boolean;
};

export type AcademicPeriodCatalog = {
  institutionId: string;
  teacherUserId: string;
  periods: AcademicPeriod[];
};

export type FetchAcademicPeriodsInput = {
  institutionId: string;
  teacherUserId: string;
  signal?: AbortSignal;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080';

export async function fetchAcademicPeriods({
  institutionId,
  teacherUserId,
  signal,
}: FetchAcademicPeriodsInput): Promise<AcademicPeriodCatalog> {
  const query = new URLSearchParams({
    institutionId,
    teacherUserId,
  });

  const response = await fetch(
    `${apiBaseUrl}/api/v1/academic-periods?${query.toString()}`,
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
      `No se pudieron cargar los períodos académicos (${response.status}).`,
    );
  }

  return response.json();
}
