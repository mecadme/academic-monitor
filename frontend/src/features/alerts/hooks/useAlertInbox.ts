import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import {
  type AlertAttentionState,
  type AlertInbox,
  fetchAlertInbox,
} from '../api/fetchAlertInbox';
import {
  acknowledgeAlert,
  markAlertPending,
} from '../api/triageAlert';

type UseAlertInboxInput = {
  institutionId: string | null;
  teacherUserId: string | null;
  courseId?: string | null;
  academicPeriodId?: string | null;
  attentionState?: AlertAttentionState;
};

type AlertTriageAction = 'ACKNOWLEDGE' | 'MARK_PENDING';

type AlertActionError = {
  alertId: string;
  action: AlertTriageAction;
  message: string;
};

export function useAlertInbox({
  institutionId,
  teacherUserId,
  courseId = null,
  academicPeriodId = null,
  attentionState = 'PENDING',
}: UseAlertInboxInput) {
  const [inbox, setInbox] =
    useState<AlertInbox | null>(null);
  const [loading, setLoading] =
    useState(false);
  const [error, setError] =
    useState<string | null>(null);
  const [actionError, setActionError] =
    useState<AlertActionError | null>(null);
  const [actionAlertIds, setActionAlertIds] =
    useState<ReadonlySet<string>>(new Set());

  const activeController =
    useRef<AbortController | null>(null);
  const requestSequence = useRef(0);
  const actionsInFlight = useRef(new Set<string>());
  const requestScope = [
    institutionId,
    teacherUserId,
    courseId,
    academicPeriodId,
    attentionState,
  ].join(':');
  const requestScopeRef = useRef(requestScope);
  requestScopeRef.current = requestScope;

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
        attentionState,
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
    attentionState,
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

  useEffect(() => {
    setActionError(null);
  }, [requestScope]);

  const performAction = useCallback(
    async (alertId: string, action: AlertTriageAction) => {
      if (
        !institutionId ||
        !teacherUserId ||
        actionsInFlight.current.has(alertId)
      ) {
        return;
      }

      const actionScope = requestScopeRef.current;
      actionsInFlight.current.add(alertId);
      setActionAlertIds(new Set(actionsInFlight.current));
      setActionError(null);

      try {
        const input = {
          alertId,
          institutionId,
          teacherUserId,
        };

        if (action === 'ACKNOWLEDGE') {
          await acknowledgeAlert(input);
        } else {
          await markAlertPending(input);
        }

        if (requestScopeRef.current === actionScope) {
          await loadInbox();
        }
      } catch (err) {
        if (requestScopeRef.current === actionScope) {
          setActionError({
            alertId,
            action,
            message:
              err instanceof Error
                ? err.message
                : 'No se pudo actualizar la atención de la alerta.',
          });
        }
      } finally {
        actionsInFlight.current.delete(alertId);
        setActionAlertIds(new Set(actionsInFlight.current));
      }
    },
    [institutionId, loadInbox, teacherUserId],
  );

  const acknowledge = useCallback(
    async (alertId: string) => {
      await performAction(alertId, 'ACKNOWLEDGE');
    },
    [performAction],
  );

  const markPending = useCallback(
    async (alertId: string) => {
      await performAction(alertId, 'MARK_PENDING');
    },
    [performAction],
  );

  const retryAction = useCallback(async () => {
    if (actionError) {
      await performAction(actionError.alertId, actionError.action);
    }
  }, [actionError, performAction]);

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
    actionError: actionError?.message ?? null,
    actionAlertIds,
    acknowledge,
    markPending,
    retryAction,
  };
}
