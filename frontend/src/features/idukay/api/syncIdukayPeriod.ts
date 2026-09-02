export type SyncIdukayPeriodInput = {
  institutionId: string
  teacherUserId: string
  periodExternalId: string
}

export type SyncIdukayPeriodResponse = {
  academicPeriodId: string
  coursesProcessed: number
  gradesProcessed: number
  openAlerts: number
  warnings: number
  critical: number
}

export async function syncIdukayPeriod(
  input: SyncIdukayPeriodInput,
): Promise<SyncIdukayPeriodResponse> {
  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ??
    'http://localhost:8080'

  const query = new URLSearchParams({
    institutionId: input.institutionId,
    teacherUserId: input.teacherUserId,
    periodExternalId: input.periodExternalId,
  })

  const response = await fetch(
    `${apiBaseUrl}/api/v1/integrations/idukay/test-sync?${query.toString()}`,
    {
      method: 'POST',
    },
  )

  if (!response.ok) {
    const body = await response.text()

    throw new Error(
      `Idukay sync failed: ${response.status} ${body}`,
    )
  }

  return response.json()
}
