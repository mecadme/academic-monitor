import { apiFetch } from '../../../api/apiFetch';

export type AcademicPeriod = {
  id: string;
  academicYearId: string;
  externalId: string;
  name: string;
  academicYear?: string | null;
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
  signal?: AbortSignal;
};


export async function fetchAcademicPeriods({
  signal,
}: FetchAcademicPeriodsInput): Promise<AcademicPeriodCatalog> {

  const response = await apiFetch(
    `/api/v1/academic-periods`,
    {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    },
    { errorMessage: (status) => `No se pudieron cargar los períodos académicos (${status}).` },
  );


  return response.json();
}
