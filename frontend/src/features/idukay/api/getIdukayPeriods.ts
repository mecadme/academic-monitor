import { apiFetch } from '../../../api/apiFetch';

export type IdukayPeriod = {
  id: string
  name: string
  abbreviation: string
}

export type IdukayPeriodsResponse = {
  academicYearId: string
  academicYear: string
  baseScore: number
  periods: IdukayPeriod[]
}

export async function getIdukayPeriods(
): Promise<IdukayPeriodsResponse> {


  const response = await apiFetch(
    `/api/v1/integrations/idukay/test-periods`,
    {
      method: "GET",
    },
  )


  return response.json()
}
