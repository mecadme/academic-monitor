import { useEffect, useState } from 'react';

import {
  type DashboardResult,
  type DemoScenario,
  getDashboard,
  syncDemo,
} from '../../../api/demo';

export function useDemoDashboard() {
  const [dashboard, setDashboard] =
    useState<DashboardResult | null>(null);

  const [teacherUserId, setTeacherUserId] =
    useState<string | null>(null);

  const [institutionId, setInstitutionId] =
    useState<string | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [syncing, setSyncing] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  async function loadScenario(
    scenario: DemoScenario,
  ) {
    try {
      setSyncing(true);
      setError(null);

      const sync =
        await syncDemo(scenario);

      setInstitutionId(
        sync.institutionId,
      );

      setTeacherUserId(
        sync.teacherUserId,
      );

      const data =
        await getDashboard(
          sync.teacherUserId,
        );

      setDashboard(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Ocurrió un error inesperado.',
      );
    } finally {
      setSyncing(false);
      setLoading(false);
    }
  }

  async function refreshDashboard() {
    try {
      if (!teacherUserId) {
        return;
      }

      setError(null);

      const data =
        await getDashboard(
          teacherUserId,
        );

      setDashboard(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'No se pudo actualizar el dashboard.',
      );
    }
  }

  useEffect(() => {
    loadScenario('INITIAL');
  }, []);

  return {
    dashboard,
    teacherUserId,
    institutionId,
    loading,
    syncing,
    error,
    loadScenario,
    refreshDashboard,
  };
}
