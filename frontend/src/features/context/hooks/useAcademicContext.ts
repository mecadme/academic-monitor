import { useCallback, useEffect, useState } from 'react';

import { ApiError, initializeCsrf, sessionExpiredEvent } from '../../../api/apiFetch';
import { fetchSession, login, logout, type AuthSession, type LoginInput } from '../../auth/api/auth';

export function useAcademicContext() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const expired = () => { if (active) { setSession(null); setError(null); } };
    window.addEventListener(sessionExpiredEvent, expired);
    async function restore() {
      try {
        await initializeCsrf();
        const current = await fetchSession();
        if (active) setSession(current);
      } catch (err) {
        if (active && !(err instanceof ApiError && err.status === 401)) setError('No se pudo verificar la sesión. Comprueba tu conexión e inténtalo nuevamente.');
      } finally {
        if (active) setLoading(false);
      }
    }
    void restore();
    return () => { active = false; window.removeEventListener(sessionExpiredEvent, expired); };
  }, []);

  const signIn = useCallback(async (input: LoginInput) => {
    const current = await login(input);
    setSession(current);
    setError(null);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logout();
    } finally {
      setSession(null);
      setError(null);
    }
  }, []);

  return { session, institutionId: session?.institution.id ?? null, teacherUserId: session?.user.id ?? null, loading, error, signIn, signOut };
}
