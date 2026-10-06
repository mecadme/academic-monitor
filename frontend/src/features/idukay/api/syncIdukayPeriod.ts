import { apiFetch } from '../../../api/apiFetch';

export type SyncIdukayPeriodInput = {
  periodExternalId: string
}

export type SyncIdukayPeriodResponse = {
  academicPeriodId: string
  coursesProcessed: number
  gradesProcessed: number
  openAlerts: number
  warnings: number
  critical: number
  guardiansUpserted: number
  guardianRelationshipsUpserted: number
  guardianFetches: number
  guardianCacheHits: number
  guardianWarnings: number
  guardianSyncDurationMs: number
}

export async function syncIdukayPeriod(
  input: SyncIdukayPeriodInput,
): Promise<SyncIdukayPeriodResponse> {

  const query = new URLSearchParams({
    periodExternalId: input.periodExternalId,
  })

  const response = await apiFetch(
    `/api/v1/integrations/idukay/test-sync?${query.toString()}`,
    {
      method: 'POST',
    },
  )


  return response.json()
}
