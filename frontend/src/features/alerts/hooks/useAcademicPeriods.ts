import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import {
  type AcademicPeriodCatalog,
  fetchAcademicPeriods,
} from '../api/fetchAcademicPeriods';

type UseAcademicPeriodsInput = {
  institutionId: string | null;
  teacherUserId: string | null;
};

export function useAcademicPeriods({
  institutionId,
  teacherUserId,
}: UseAcademicPeriodsInput) {
  const [catalog, setCatalog] =
    useState<AcademicPeriodCatalog | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const activeController = useRef<AbortController | null>(null);
  const requestSequence = useRef(0);

  const loadPeriods = useCallback(async () => {
    activeController.current?.abort();
    const requestId = ++requestSequence.current;

    if (!institutionId || !teacherUserId) {
      activeController.current = null;
      setCatalog(null);
      setLoading(false);
      setError(null);
      return null;
    }

    const controller = new AbortController();
    activeController.current = controller;

    try {
      setLoading(true);
      setError(null);

      const result = await fetchAcademicPeriods({
        institutionId,
        teacherUserId,
        signal: controller.signal,
      });

      if (
        !controller.signal.aborted &&
        requestId === requestSequence.current
      ) {
        setCatalog(result);
        return result;
      }
    } catch (err) {
      if (
        controller.signal.aborted ||
        requestId !== requestSequence.current
      ) {
        return null;
      }

      setError(
        err instanceof Error
          ? err.message
          : 'No se pudieron cargar los períodos académicos.',
      );
    } finally {
      if (
        !controller.signal.aborted &&
        requestId === requestSequence.current
      ) {
        activeController.current = null;
        setLoading(false);
      }
    }

    return null;
  }, [institutionId, teacherUserId]);

  useEffect(() => {
    void loadPeriods();

    return () => {
      activeController.current?.abort();
    };
  }, [loadPeriods]);

  const refresh = useCallback(async () => {
    return loadPeriods();
  }, [loadPeriods]);

  const scopedCatalog =
    catalog?.institutionId === institutionId &&
    catalog.teacherUserId === teacherUserId
      ? catalog
      : null;

  return {
    catalog: scopedCatalog,
    loading,
    error,
    refresh,
  };
}
