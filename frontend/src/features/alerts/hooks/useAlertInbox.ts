import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import {
  type AlertInbox,
  fetchAlertInbox,
} from '../api/fetchAlertInbox';

type UseAlertInboxInput = {
  institutionId: string | null;
  teacherUserId: string | null;
  courseId?: string | null;
  academicPeriodId?: string | null;
};

export function useAlertInbox({
  institutionId,
  teacherUserId,
  courseId = null,
  academicPeriodId = null,
}: UseAlertInboxInput) {
  const [inbox, setInbox] =
    useState<AlertInbox | null>(null);
  const [loading, setLoading] =
    useState(false);
  const [error, setError] =
    useState<string | null>(null);

  const activeController =
    useRef<AbortController | null>(null);
  const requestSequence = useRef(0);

  const loadInbox = useCallback(async () => {
    activeController.current?.abort();
    const requestId = ++requestSequence.current;

    if (!institutionId || !teacherUserId) {
      activeController.current = null;
      setInbox(null);
      setLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();
    activeController.current = controller;

    try {
      setLoading(true);
      setError(null);

      const result = await fetchAlertInbox({
        institutionId,
        teacherUserId,
        courseId,
        academicPeriodId,
        signal: controller.signal,
      });

      if (
        !controller.signal.aborted &&
        requestId === requestSequence.current
      ) {
        setInbox(result);
      }
    } catch (err) {
      if (
        controller.signal.aborted ||
        requestId !== requestSequence.current
      ) {
        return;
      }

      setError(
        err instanceof Error
          ? err.message
          : 'No se pudieron cargar las alertas.',
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
  }, [
    academicPeriodId,
    courseId,
    institutionId,
    teacherUserId,
  ]);

  useEffect(() => {
    void loadInbox();

    return () => {
      activeController.current?.abort();
    };
  }, [loadInbox]);

  const refresh = useCallback(async () => {
    await loadInbox();
  }, [loadInbox]);

  const scopedInbox =
    inbox?.institutionId === institutionId &&
    inbox.teacherUserId === teacherUserId
      ? inbox
      : null;

  return {
    inbox: scopedInbox,
    loading,
    error,
    refresh,
  };
}
