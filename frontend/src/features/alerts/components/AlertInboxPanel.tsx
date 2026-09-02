import type { AcademicDashboardCourse } from '../../dashboard/api/fetchAcademicDashboard';
import type { AcademicPeriod } from '../api/fetchAcademicPeriods';
import type {
  AlertInbox,
  AlertInboxItem,
  AlertSeverity,
} from '../api/fetchAlertInbox';
import {
  formatAlertDueDate,
  formatAlertScore,
} from '../lib/formatAlert';

type AlertInboxPanelProps = {
  courses: AcademicDashboardCourse[];
  periods: AcademicPeriod[];
  inbox: AlertInbox | null;
  loading: boolean;
  error: string | null;
  selectedCourseId: string | null;
  selectedAcademicPeriodId: string | null;
  onCourseChange: (courseId: string | null) => void;
  onAcademicPeriodChange: (
    academicPeriodId: string | null,
  ) => void;
  onRetry: () => void | Promise<void>;
};

export function AlertInboxPanel({
  courses,
  periods,
  inbox,
  loading,
  error,
  selectedCourseId,
  selectedAcademicPeriodId,
  onCourseChange,
  onAcademicPeriodChange,
  onRetry,
}: AlertInboxPanelProps) {
  const alerts = inbox?.alerts ?? [];
  const total = inbox?.total ?? 0;

  return (
    <section className="container panel alert-inbox-panel">
      <div className="panel-header alert-inbox-header">
        <div className="alert-inbox-heading">
          <p className="section-label">
            Bandeja académica
          </p>

          <div className="alert-inbox-title-line">
            <h3 className="panel-title">
              Alertas que requieren atención
            </h3>

            <span
              className="alert-open-count"
              aria-live="polite"
            >
              {total}{' '}
              {total === 1 ? 'abierta' : 'abiertas'}
            </span>
          </div>
        </div>

        <div className="alert-inbox-filters">
          <label
            className="alert-filter"
            htmlFor="alert-course-filter"
          >
            Curso

            <select
              id="alert-course-filter"
              value={selectedCourseId ?? ''}
              onChange={(event) => {
                onCourseChange(
                  event.target.value || null,
                );
              }}
            >
              <option value="">
                Todos los cursos
              </option>

              {courses.map((course) => (
                <option
                  key={course.id}
                  value={course.id}
                >
                  {course.name}
                  {course.subject
                    ? ` — ${course.subject}`
                    : ''}
                </option>
              ))}
            </select>
          </label>

          <label
            className="alert-filter"
            htmlFor="alert-period-filter"
          >
            Período

            <select
              id="alert-period-filter"
              value={selectedAcademicPeriodId ?? ''}
              onChange={(event) => {
                onAcademicPeriodChange(
                  event.target.value || null,
                );
              }}
            >
              <option value="">
                Todos los períodos
              </option>

              {periods.map((period) => (
                <option
                  key={period.id}
                  value={period.id}
                >
                  {period.abbreviation
                    ? `${period.abbreviation} — `
                    : ''}
                  {period.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {selectedAcademicPeriodId === null && (
        <p className="alert-scope-note">
          Incluye alertas abiertas de períodos anteriores.
        </p>
      )}

      {loading && (
        <p
          className="alert-refresh-status"
          role="status"
        >
          {inbox
            ? 'Actualizando alertas…'
            : 'Cargando alertas…'}
        </p>
      )}

      {error && (
        <div
          className="alert-local-error"
          role="alert"
        >
          <p>{error}</p>

          <button
            className="btn btn-secondary"
            type="button"
            onClick={() => {
              void onRetry();
            }}
          >
            Reintentar
          </button>
        </div>
      )}

      {!loading && !error && inbox && alerts.length === 0 && (
        <p className="alert-empty-state">
          {emptyStateMessage(
            selectedCourseId,
            selectedAcademicPeriodId,
          )}
        </p>
      )}

      {alerts.length > 0 && (
        <div
          className="alert-list"
          aria-label="Alertas abiertas"
        >
          {alerts.map((alert) => (
            <AlertRow
              key={alert.id}
              alert={alert}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function emptyStateMessage(
  courseId: string | null,
  academicPeriodId: string | null,
) {
  if (courseId && academicPeriodId) {
    return 'No hay alertas abiertas para este curso y período.';
  }

  if (courseId) {
    return 'No hay alertas abiertas para este curso.';
  }

  if (academicPeriodId) {
    return 'No hay alertas abiertas para este período.';
  }

  return 'No hay alertas abiertas.';
}

function AlertRow({
  alert,
}: {
  alert: AlertInboxItem;
}) {
  const score = formatAlertScore(alert.score);
  const maximumScore = formatAlertScore(
    alert.activity.maximumScore,
  );

  return (
    <article className="alert-row">
      <div className="alert-identity">
        <SeverityBadge severity={alert.severity} />

        <h4>{alert.student.name}</h4>
      </div>

      <div className="alert-details">
        <p className="alert-activity-name">
          {alert.activity.name}
        </p>

        <p className="alert-course-name">
          <span>{alert.course.name}</span>

          {alert.course.subject && (
            <>
              <span aria-hidden="true"> · </span>
              <span>{alert.course.subject}</span>
            </>
          )}
        </p>

        {alert.activity.dueDate && (
          <p className="alert-due-date">
            Entrega:{' '}
            {formatAlertDueDate(
              alert.activity.dueDate,
            )}
          </p>
        )}
      </div>

      <p
        className="alert-score"
        aria-label={`Calificación ${score} de ${maximumScore}`}
      >
        <strong>{score}</strong>
        <span> / {maximumScore}</span>
      </p>
    </article>
  );
}

function SeverityBadge({
  severity,
}: {
  severity: AlertSeverity;
}) {
  const label =
    severity === 'CRITICAL'
      ? 'Crítica'
      : 'Advertencia';

  return (
    <span
      className={`alert-severity alert-severity-${severity.toLowerCase()}`}
    >
      {label}
    </span>
  );
}
