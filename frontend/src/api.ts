import { apiFetch } from './api/apiFetch';
import type { SystemHealth } from './types';


export async function fetchSystemHealth(): Promise<SystemHealth> {
  const response = await apiFetch(`/api/v1/health`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Health endpoint returned ${response.status}`);
  }

  return response.json() as Promise<SystemHealth>;
}
