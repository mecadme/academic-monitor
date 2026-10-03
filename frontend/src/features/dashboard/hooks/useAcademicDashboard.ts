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
  academicPeriodId?: string | null;
};

export function useAcademicDashboard({
  institutionId,
  teacherUserId,
  academicPeriodId = null,
}: UseAcademicDashboardInput) {
  const [dashboard, setDashboard] =
    useState<AcademicDashboard | null>(null);
  const [loadedScope, setLoadedScope] =
    useState<string | null>(null);
  const [loading, setLoading] =
    useState(false);
  const [error, setError] =
    useState<string | null>(null);

  const loadDashboard = useCallback(
    async (signal?: AbortSignal) => {
      if (!institutionId || !teacherUserId) {
        setDashboard(null);
        setLoadedScope(null);
        setLoading(false);
        setError(null);
        return;
      }

      try {
        setDashboard(null);
        setLoadedScope(null);
        setLoading(true);
        setError(null);

        const result =
          await fetchAcademicDashboard({
            institutionId,
            teacherUserId,
            academicPeriodId,
            signal,
          });

        if (!signal?.aborted) {
          setDashboard(result);
          setLoadedScope(
            [institutionId, teacherUserId, academicPeriodId ?? 'none'].join(':'),
          );
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
    [academicPeriodId, institutionId, teacherUserId],
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

  const requestedScope =
    [institutionId, teacherUserId, academicPeriodId ?? 'none'].join(':');
  const scopedDashboard =
    dashboard?.institutionId === institutionId &&
    dashboard.teacherUserId === teacherUserId &&
    loadedScope === requestedScope
      ? dashboard
      : null;

  return {
    dashboard: scopedDashboard,
    loading,
    error,
    refresh,
  };
}
