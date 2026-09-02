import './App.css';

import { ActivityPanel } from './features/dashboard/components/ActivityPanel';
import { DashboardHero } from './features/dashboard/components/DashboardHero';
import { DashboardSummary } from './features/dashboard/components/DashboardSummary';
import { useDemoDashboard } from './features/dashboard/hooks/useDemoDashboard';

import { IdukayIntegrationCard } from './features/dashboard/components/IdukayIntegrationCard';
import { useIdukayIntegration } from './features/idukay/hooks/useIdukayIntegration';

import { AppHeader } from './components/layout/AppHeader';
import { useAcademicContext } from './features/context/hooks/useAcademicContext';

function App() {
  const context = useAcademicContext();
  const demo = useDemoDashboard();

  const idukay = useIdukayIntegration({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
  });

  if (demo.loading) {
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

  if (demo.error && !demo.dashboard) {
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
            {demo.error}
          </p>

          <button
            className="btn btn-primary"
            onClick={() =>
              demo.loadScenario('INITIAL')
            }
          >
            Reintentar
          </button>
        </div>
      </main>
    );
  }

  if (!demo.dashboard) {
    return null;
  }

  const displayedError =
    demo.error ?? context.error ?? idukay.error;

  return (
    <main className="app-shell">
      <AppHeader
        syncing={demo.syncing}
        onInitialSync={() =>
          demo.loadScenario('INITIAL')
        }
        onImprovement={() =>
          demo.loadScenario('IMPROVED')
        }
      />

      <DashboardHero
        dashboard={demo.dashboard}
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
        summary={demo.dashboard.summary}
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
        dashboard={demo.dashboard}
        onRefresh={demo.refreshDashboard}
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
