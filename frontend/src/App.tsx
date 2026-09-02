import './App.css';

import { AcademicCoursePanel } from './features/dashboard/components/AcademicCoursePanel';
import { DashboardHero } from './features/dashboard/components/DashboardHero';
import { DashboardSummary } from './features/dashboard/components/DashboardSummary';
import { useAcademicDashboard } from './features/dashboard/hooks/useAcademicDashboard';

import { IdukayIntegrationCard } from './features/dashboard/components/IdukayIntegrationCard';
import { useIdukayIntegration } from './features/idukay/hooks/useIdukayIntegration';

import { AppHeader } from './components/layout/AppHeader';
import { useAcademicContext } from './features/context/hooks/useAcademicContext';

function App() {
  const context = useAcademicContext();
  const dashboard = useAcademicDashboard({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
  });

  const idukay = useIdukayIntegration({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
    onSyncSuccess: dashboard.refresh,
  });

  if (context.loading) {
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
            Inicializando contexto académico...
          </p>
        </div>
      </main>
    );
  }

  if (context.error) {
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
            {context.error}
          </p>
        </div>
      </main>
    );
  }

  if (dashboard.loading && !dashboard.dashboard) {
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
            Cargando dashboard académico...
          </p>
        </div>
      </main>
    );
  }

  if (dashboard.error && !dashboard.dashboard) {
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
            {dashboard.error}
          </p>

          <button
            className="btn btn-primary"
            onClick={dashboard.refresh}
          >
            Reintentar
          </button>
        </div>
      </main>
    );
  }

  if (!dashboard.dashboard) {
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
            Cargando dashboard académico...
          </p>
        </div>
      </main>
    );
  }

  const displayedError =
    dashboard.error ?? idukay.error;

  return (
    <main className="app-shell">
      <AppHeader />

      <DashboardHero
        dashboard={dashboard.dashboard}
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
        summary={dashboard.dashboard.summary}
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

      <AcademicCoursePanel
        courses={dashboard.dashboard.courses}
        refreshing={dashboard.loading}
        onRefresh={dashboard.refresh}
      />
    </main>
  );
}

export default App;
