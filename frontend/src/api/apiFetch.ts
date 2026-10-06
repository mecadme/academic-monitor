const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const sessionExpiredEvent = 'academic-monitor:session-expired';
export type ApiProblem = {
  detail?: string;
  code?: string;
  institutions?: Array<{ institutionId: string; institutionName: string; institutionRole: 'ADMIN' | 'TEACHER' }>;
};
export class ApiError extends Error {
  constructor(public readonly status: number, public readonly problem: ApiProblem, fallback: string) {
    super(problem.detail || fallback);
    this.name = 'ApiError';
  }
}
let refreshPromise: Promise<void> | null = null;
let csrfPromise: Promise<void> | null = null;
let refreshGeneration = 0;

function csrfToken() {
  const value = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='));
  return value ? decodeURIComponent(value.slice('XSRF-TOKEN='.length)) : null;
}

export async function initializeCsrf(): Promise<void> {
  csrfPromise ??= apiFetch('/api/v1/auth/csrf', {}, { refresh: false })
    .then(() => undefined).finally(() => { csrfPromise = null; });
  await csrfPromise;
}

async function refreshSession() {
  refreshPromise ??= apiFetch('/api/v1/auth/refresh', { method: 'POST' }, { refresh: false })
    .then(() => { refreshGeneration++; })
    .catch((error: unknown) => {
      // A network failure must not discard an otherwise recoverable session.
      if (error instanceof ApiError && error.status === 401) window.dispatchEvent(new Event(sessionExpiredEvent));
      throw error;
    })
    .finally(() => { refreshPromise = null; });
  await refreshPromise;
}

type Options = { refresh?: boolean; errorMessage?: string | ((status: number) => string) };

export async function apiFetch(path: string, init: RequestInit = {}, options: Options = {}): Promise<Response> {
  const mutable = !['GET', 'HEAD', 'OPTIONS'].includes((init.method ?? 'GET').toUpperCase());
  if (mutable && !csrfToken()) await initializeCsrf();
  const generation = refreshGeneration;
  const send = () => {
    const headers = new Headers(init.headers);
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');
    const csrf = csrfToken();
    if (mutable && csrf) headers.set('X-XSRF-TOKEN', csrf);
    return fetch(`${apiBaseUrl}${path}`, { ...init, credentials: 'include', headers: Object.fromEntries(headers.entries()) });
  };
  let response = await send();
  if (response.status === 401 && options.refresh !== false) {
    if (generation === refreshGeneration) await refreshSession();
    init.signal?.throwIfAborted();
    response = await send();
    if (response.status === 401) window.dispatchEvent(new Event(sessionExpiredEvent));
  }
  if (!response.ok) {
    let problem: ApiProblem = {};
    try { problem = await response.json() as ApiProblem; } catch { /* Proxies may return an empty or non-JSON error. */ }
    const fallback = typeof options.errorMessage === 'function' ? options.errorMessage(response.status)
      : options.errorMessage ?? 'No se pudo completar la solicitud.';
    throw new ApiError(response.status, problem, fallback);
  }
  return response;
}
