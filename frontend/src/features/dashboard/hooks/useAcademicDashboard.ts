import {
  useCallback,
  useEffect,
  useState,
} from 'react';

import {
  type AcademicDashboard,
  fetchAcademicDashboard,
} from '../api/fetchAcademicDashboard';

type UseAcademicDashboardInput = {
  institutionId: string | null;
  teacherUserId: string | null;
};

export function useAcademicDashboard({
  institutionId,
  teacherUserId,
}: UseAcademicDashboardInput) {
  const [dashboard, setDashboard] =
    useState<AcademicDashboard | null>(null);
  const [loading, setLoading] =
    useState(false);
  const [error, setError] =
    useState<string | null>(null);

  const loadDashboard = useCallback(
    async (signal?: AbortSignal) => {
      if (!institutionId || !teacherUserId) {
        setDashboard(null);
        setLoading(false);
        setError(null);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const result =
          await fetchAcademicDashboard({
            institutionId,
            teacherUserId,
            signal,
          });

        if (!signal?.aborted) {
          setDashboard(result);
        }
      } catch (err) {
        if (signal?.aborted) {
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : 'No se pudo cargar el dashboard académico.',
        );
      } finally {
        if (!signal?.aborted) {
          setLoading(false);
        }
      }
    },
    [institutionId, teacherUserId],
  );

  useEffect(() => {
    const controller = new AbortController();

    void loadDashboard(controller.signal);

    return () => {
      controller.abort();
    };
  }, [loadDashboard]);

  const refresh = useCallback(
    async () => {
      await loadDashboard();
    },
    [loadDashboard],
  );

  const scopedDashboard =
    dashboard?.institutionId === institutionId &&
    dashboard.teacherUserId === teacherUserId
      ? dashboard
      : null;

  return {
    dashboard: scopedDashboard,
    loading,
    error,
    refresh,
  };
}
