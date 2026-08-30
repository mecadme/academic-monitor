import { useEffect, useState } from 'react';
import './App.css';

import {
  type DashboardResult,
  getDashboard,
  syncDemo,
} from './api/demo';

import { ActivityPanel } from './features/dashboard/components/ActivityPanel';
import { DashboardHero } from './features/dashboard/components/DashboardHero';
import { DashboardSummary } from './features/dashboard/components/DashboardSummary';

import { IdukayIntegrationCard } from './features/dashboard/components/IdukayIntegrationCard';
import { useIdukayIntegration } from './features/idukay/hooks/useIdukayIntegration';

import { AppHeader } from './components/layout/AppHeader';

function App() {
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

  const idukay = useIdukayIntegration({
    institutionId,
    teacherUserId,
  });

  async function loadScenario(
    scenario: 'INITIAL' | 'IMPROVED',
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

  if (loading) {
    return (
      <main className="app-shell loading-shell">
        <div className="loading-card">
          <div className="loading-mark">
            AM
          </div>

          <h1>
            Academic Monitor
          </h1>

          <p>
            Preparando entorno de demostración...
          </p>
        </div>
      </main>
    );
  }

  if (error && !dashboard) {
    return (
      <main className="app-shell loading-shell">
        <div className="loading-card">
          <div className="loading-mark">
            !
          </div>

          <h1>
            No se pudo cargar Academic Monitor
          </h1>

          <p>
            {error}
          </p>

          <button
            className="btn btn-primary"
            onClick={() =>
              loadScenario('INITIAL')
            }
          >
            Reintentar
          </button>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return null;
  }

  const displayedError =
    error ?? idukay.error;

  return (
    <main className="app-shell">
      <AppHeader
        syncing={syncing}
        onInitialSync={() =>
          loadScenario('INITIAL')
        }
        onImprovement={() =>
          loadScenario('IMPROVED')
        }
      />

      <DashboardHero
        dashboard={dashboard}
      />

      {displayedError && (
        <section className="container">
          <div className="error-banner">
            <span className="error-icon">
              !
            </span>

            <span>
              {displayedError}
            </span>
          </div>
        </section>
      )}

      <DashboardSummary
        summary={dashboard.summary}
      />

      <IdukayIntegrationCard
        connected={
          idukay.connected
        }
        connecting={
          idukay.connecting
        }
        syncing={
          idukay.syncing
        }
        email={
          idukay.email
        }
        password={
          idukay.password
        }
        academicYear={
          idukay.academicYear
        }
        baseScore={
          idukay.baseScore
        }
        periods={
          idukay.periods
        }
        selectedPeriodId={
          idukay.selectedPeriodId
        }
        syncResult={
          idukay.syncResult
        }
        onEmailChange={
          idukay.setEmail
        }
        onPasswordChange={
          idukay.setPassword
        }
        onConnect={
          idukay.connect
        }
        onPeriodChange={
          idukay.selectPeriod
        }
        onSync={
          idukay.synchronizeSelectedPeriod
        }
      />

      <ActivityPanel
        dashboard={dashboard}
        onRefresh={refreshDashboard}
      />

      <section className="container footer-note">
        <div className="footer-note-card">
          <div className="footer-note-icon">
            i
          </div>

          <p>
            <strong>
              Entorno de demostración.
            </strong>{' '}

            “Sincronizar demo” carga el
            escenario inicial y “Simular
            mejora” permite observar la
            resolución automática de alertas.
          </p>
        </div>
      </section>
    </main>
  );
}

export default App;
