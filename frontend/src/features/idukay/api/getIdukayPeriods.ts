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

export type GetIdukayPeriodsInput = {
  institutionId: string
  teacherUserId: string
}

export async function getIdukayPeriods(
  input: GetIdukayPeriodsInput,
): Promise<IdukayPeriodsResponse> {
  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ??
    "http://localhost:8080"

  const query = new URLSearchParams({
    institutionId: input.institutionId,
    teacherUserId: input.teacherUserId,
  })

  const response = await fetch(
    `${apiBaseUrl}/api/v1/integrations/idukay/test-periods?${query.toString()}`,
    {
      method: "GET",
    },
  )

  if (!response.ok) {
    const body = await response.text()

    throw new Error(
      `Idukay periods request failed: ${response.status} ${body}`,
    )
  }

  return response.json()
}
