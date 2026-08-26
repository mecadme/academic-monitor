import type { SystemHealth } from './types';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function fetchSystemHealth(): Promise<SystemHealth> {
  const response = await fetch(`${apiBaseUrl}/api/v1/health`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Health endpoint returned ${response.status}`);
  }

  return response.json() as Promise<SystemHealth>;
}
