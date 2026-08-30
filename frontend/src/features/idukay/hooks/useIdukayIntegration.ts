import { useEffect, useState } from 'react';

import {
  getIdukayPeriods,
  type IdukayPeriod,
} from '../api/getIdukayPeriods';

import { testIdukayLogin } from '../api/testIdukayLogin';

import {
  syncIdukayPeriod,
  type SyncIdukayPeriodResponse,
} from '../api/syncIdukayPeriod';

type UseIdukayIntegrationInput = {
  institutionId: string | null;
  teacherUserId: string | null;
};

export function useIdukayIntegration({
                                       institutionId,
                                       teacherUserId,
                                     }: UseIdukayIntegrationInput) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const [connected, setConnected] =
    useState(false);

  const [connecting, setConnecting] =
    useState(false);

  const [syncing, setSyncing] =
    useState(false);

  const [academicYear, setAcademicYear] =
    useState<string | null>(null);

  const [baseScore, setBaseScore] =
    useState<number | null>(null);

  const [periods, setPeriods] =
    useState<IdukayPeriod[]>([]);

  const [selectedPeriodId, setSelectedPeriodId] =
    useState('');

  const [syncResult, setSyncResult] =
    useState<SyncIdukayPeriodResponse | null>(
      null,
    );

  const [error, setError] =
    useState<string | null>(null);

  async function connect() {
    if (!institutionId || !teacherUserId) {
      setError(
        'No hay contexto académico disponible.',
      );
      return;
    }

    if (!email.trim() || !password) {
      setError(
        'Ingresa el correo y la contraseña de Idukay.',
      );
      return;
    }

    try {
      setConnecting(true);
      setError(null);

      await testIdukayLogin({
        email,
        password,
        institutionId,
        teacherUserId,
      });

      const periodResult =
        await getIdukayPeriods({
          institutionId,
          teacherUserId,
        });

      setAcademicYear(
        periodResult.academicYear,
      );

      setBaseScore(
        periodResult.baseScore,
      );

      setPeriods(
        periodResult.periods,
      );

      setSelectedPeriodId(
        periodResult.periods[0]?.id ?? '',
      );

      setSyncResult(null);
      setConnected(true);

      // No conservamos la contraseña
      // después del login exitoso.
      setPassword('');
    } catch (err) {
      setConnected(false);

      setError(
        err instanceof Error
          ? err.message
          : 'No se pudo conectar con Idukay.',
      );
    } finally {
      setConnecting(false);
    }
  }

  async function synchronizeSelectedPeriod() {
    if (
      !institutionId ||
      !teacherUserId ||
      !selectedPeriodId
    ) {
      setError(
        'No hay un período académico válido para sincronizar.',
      );
      return;
    }

    try {
      setSyncing(true);
      setError(null);
      setSyncResult(null);

      const result =
        await syncIdukayPeriod({
          institutionId,
          teacherUserId,
          periodExternalId:
          selectedPeriodId,
        });

      setSyncResult(result);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'No se pudo sincronizar el período de Idukay.',
      );
    } finally {
      setSyncing(false);
    }
  }

  function selectPeriod(
    periodId: string,
  ) {
    setSelectedPeriodId(periodId);

    // Evitamos mostrar resultados
    // correspondientes al período anterior.
    setSyncResult(null);
    setError(null);
  }

  useEffect(() => {
    setConnected(false);
    setConnecting(false);
    setSyncing(false);

    setAcademicYear(null);
    setBaseScore(null);

    setPeriods([]);
    setSelectedPeriodId('');

    setSyncResult(null);
    setError(null);

    setPassword('');
  }, [
    institutionId,
    teacherUserId,
  ]);

  return {
    email,
    password,

    connected,
    connecting,
    syncing,

    academicYear,
    baseScore,

    periods,
    selectedPeriodId,

    syncResult,
    error,

    setEmail,
    setPassword,

    connect,
    selectPeriod,
    synchronizeSelectedPeriod,
  };
}
