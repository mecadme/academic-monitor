import './App.css';

import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import { AlertInboxPanel } from './features/alerts/components/AlertInboxPanel';
import type { AlertAttentionState } from './features/alerts/api/fetchAlertInbox';
import { useAcademicPeriods } from './features/alerts/hooks/useAcademicPeriods';
import { useAlertInbox } from './features/alerts/hooks/useAlertInbox';
import { AcademicCoursePanel } from './features/dashboard/components/AcademicCoursePanel';
import { DashboardHero } from './features/dashboard/components/DashboardHero';
import { DashboardSummary } from './features/dashboard/components/DashboardSummary';
import { useAcademicDashboard } from './features/dashboard/hooks/useAcademicDashboard';

import { IdukayIntegrationCard } from './features/dashboard/components/IdukayIntegrationCard';
import type { SyncIdukayPeriodResponse } from './features/idukay/api/syncIdukayPeriod';
import { useIdukayIntegration } from './features/idukay/hooks/useIdukayIntegration';

import { AppHeader } from './components/layout/AppHeader';
import { useAcademicContext } from './features/context/hooks/useAcademicContext';

function App() {
  const [selectedAlertCourseId, setSelectedAlertCourseId] =
    useState<string | null>(null);
  const [selectedAcademicPeriodId, setSelectedAcademicPeriodId] =
    useState<string | null>(null);
  const [selectedAlertAttentionState, setSelectedAlertAttentionState] =
    useState<AlertAttentionState>('PENDING');
  const [periodSelectionScope, setPeriodSelectionScope] =
    useState<string | null>(null);
  const initializedPeriodScope = useRef<string | null>(null);
  const context = useAcademicContext();
  const dashboard = useAcademicDashboard({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
  });
  const academicPeriods = useAcademicPeriods({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
  });
  const currentPeriodScope =
    context.institutionId && context.teacherUserId
      ? `${context.institutionId}:${context.teacherUserId}`
      : null;
  const periodCatalogReady =
    academicPeriods.catalog !== null &&
    periodSelectionScope === currentPeriodScope;
  const alertInbox = useAlertInbox({
    institutionId: periodCatalogReady
      ? context.institutionId
      : null,
    teacherUserId: periodCatalogReady
      ? context.teacherUserId
      : null,
    courseId: selectedAlertCourseId,
    academicPeriodId: selectedAcademicPeriodId,
    attentionState: selectedAlertAttentionState,
  });

  useEffect(() => {
    if (
      !academicPeriods.catalog ||
      !context.institutionId ||
      !context.teacherUserId
    ) {
      return;
    }

    const scopeKey = `${context.institutionId}:${context.teacherUserId}`;

    if (initializedPeriodScope.current !== scopeKey) {
      initializedPeriodScope.current = scopeKey;
      setSelectedAcademicPeriodId(
        latestSynchronizedPeriodId(
          academicPeriods.catalog.periods,
        ),
      );
      setPeriodSelectionScope(scopeKey);
      return;
    }

    if (
      selectedAcademicPeriodId &&
      !academicPeriods.catalog.periods.some(
        (period) => period.id === selectedAcademicPeriodId,
      )
    ) {
      setSelectedAcademicPeriodId(null);
    }
  }, [
    academicPeriods.catalog,
    context.institutionId,
    context.teacherUserId,
    selectedAcademicPeriodId,
  ]);

  useEffect(() => {
    if (
      selectedAlertCourseId &&
      dashboard.dashboard &&
      !dashboard.dashboard.courses.some(
        (course) => course.id === selectedAlertCourseId,
      )
    ) {
      setSelectedAlertCourseId(null);
    }
  }, [dashboard.dashboard, selectedAlertCourseId]);

  const refreshAfterSync = useCallback(
    async (result: SyncIdukayPeriodResponse) => {
      await academicPeriods.refresh();

      const periodChanged =
        selectedAcademicPeriodId !== result.academicPeriodId;
      setSelectedAcademicPeriodId(result.academicPeriodId);

      const refreshes: Promise<unknown>[] = [
        dashboard.refresh(),
      ];

      if (!periodChanged) {
        refreshes.push(alertInbox.refresh());
      }

      await Promise.allSettled(refreshes);
    },
    [
      academicPeriods.refresh,
      alertInbox.refresh,
      dashboard.refresh,
      selectedAcademicPeriodId,
    ],
  );

  const retryAlertPanel = useCallback(async () => {
    if (academicPeriods.error) {
      await academicPeriods.refresh();
    }

    if (academicPeriods.catalog) {
      await alertInbox.refresh();
    }
  }, [
    academicPeriods.catalog,
    academicPeriods.error,
    academicPeriods.refresh,
    alertInbox.refresh,
  ]);

  const idukay = useIdukayIntegration({
    institutionId: context.institutionId,
    teacherUserId: context.teacherUserId,
    onSyncSuccess: refreshAfterSync,
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

      <AlertInboxPanel
        courses={dashboard.dashboard.courses}
        periods={academicPeriods.catalog?.periods ?? []}
        inbox={alertInbox.inbox}
        loading={academicPeriods.loading || alertInbox.loading}
        error={academicPeriods.error ?? alertInbox.error}
        actionError={alertInbox.actionError}
        actionAlertIds={alertInbox.actionAlertIds}
        selectedCourseId={selectedAlertCourseId}
        selectedAcademicPeriodId={selectedAcademicPeriodId}
        attentionState={selectedAlertAttentionState}
        onCourseChange={setSelectedAlertCourseId}
        onAcademicPeriodChange={setSelectedAcademicPeriodId}
        onAttentionStateChange={setSelectedAlertAttentionState}
        onRetry={retryAlertPanel}
        onRetryAction={alertInbox.retryAction}
        onAcknowledge={alertInbox.acknowledge}
        onMarkPending={alertInbox.markPending}
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

function latestSynchronizedPeriodId(
  periods: Array<{
    id: string;
    order: number;
    synchronized: boolean;
  }>,
) {
  return periods.reduce<{
    id: string;
    order: number;
  } | null>((latest, period) => {
    if (!period.synchronized) {
      return latest;
    }

    if (!latest || period.order >= latest.order) {
      return period;
    }

    return latest;
  }, null)?.id ?? null;
}

export default App;
