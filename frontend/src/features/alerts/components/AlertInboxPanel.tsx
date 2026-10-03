import type { AcademicDashboardCourse } from '../../dashboard/api/fetchAcademicDashboard';
import type { AcademicPeriod } from '../api/fetchAcademicPeriods';
import type {
  AlertAttentionState,
  AlertInbox,
  AlertInboxItem,
  AlertSeverity,
} from '../api/fetchAlertInbox';
import {
  formatAcknowledgedAt,
  formatAlertDueDate,
  formatAlertScore,
} from '../lib/formatAlert';
import {
  type Communication,
  prepareAlertCommunication,
  saveCommunicationDraft,
  sendCommunication,
} from '../api/communications';
import { useState } from 'react';

type AlertInboxPanelProps = {
  courses: AcademicDashboardCourse[];
  periods: AcademicPeriod[];
  inbox: AlertInbox | null;
  loading: boolean;
  error: string | null;
  actionError: string | null;
  actionAlertIds: ReadonlySet<string>;
  selectedCourseId: string | null;
  selectedAcademicPeriodId: string | null;
  attentionState: AlertAttentionState;
  onCourseChange: (courseId: string | null) => void;
  onAcademicPeriodChange: (academicPeriodId: string | null) => void;
  onAttentionStateChange: (attentionState: AlertAttentionState) => void;
  onRetry: () => void | Promise<void>;
  onRetryAction: () => void | Promise<void>;
  onAcknowledge: (alertId: string) => void | Promise<void>;
  onMarkPending: (alertId: string) => void | Promise<void>;
  institutionId?: string | null;
  teacherUserId?: string | null;
};

const attentionOptions: Array<{
  value: AlertAttentionState;
  label: string;
}> = [
  { value: 'PENDING', label: 'Pendientes' },
  { value: 'ACKNOWLEDGED', label: 'Atendidas' },
  { value: 'ALL', label: 'Todas activas' },
];

export function AlertInboxPanel({
  courses,
  periods,
  inbox,
  loading,
  error,
  actionError,
  actionAlertIds,
  selectedCourseId,
  selectedAcademicPeriodId,
  attentionState,
  onCourseChange,
  onAcademicPeriodChange,
  onAttentionStateChange,
  onRetry,
  onRetryAction,
  onAcknowledge,
  onMarkPending,
  institutionId = null,
  teacherUserId = null,
}: AlertInboxPanelProps) {
  const alerts = inbox?.alerts ?? [];
  const total = inbox?.total ?? 0;
  const [communication, setCommunication] = useState<Communication | null>(null);
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [communicationError, setCommunicationError] = useState<string | null>(null);
  const [preparingAlertId, setPreparingAlertId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [sending, setSending] = useState(false);
  const [confirmingSend, setConfirmingSend] = useState(false);
  const communicationScope = institutionId && teacherUserId ? { institutionId, teacherUserId } : null;

  const prepareCommunication = async (alertId: string) => {
    if (!communicationScope || preparingAlertId) return;
    setPreparingAlertId(alertId);
    setCommunicationError(null);
    try {
      const draft = await prepareAlertCommunication(alertId, communicationScope);
      setCommunication(draft);
      setSubject(draft.subject);
      setContent(draft.content);
      setConfirmingSend(false);
    } catch (error) {
      setCommunicationError(error instanceof Error ? error.message : 'No se pudo preparar el borrador.');
    } finally {
      setPreparingAlertId(null);
    }
  };

  const saveDraft = async () => {
    if (!communication || !communicationScope || saving) return;
    setSaving(true);
    setCommunicationError(null);
    try {
      setCommunication(await saveCommunicationDraft(communication.id, communicationScope, subject, content));
    } catch (error) {
      setCommunicationError(error instanceof Error ? error.message : 'No se pudo guardar el borrador.');
    } finally {
      setSaving(false);
    }
  };

  const confirmSend = async () => {
    if (!communication || !communicationScope || sending) return;
    setSending(true);
    setCommunicationError(null);
    try {
      setCommunication(await sendCommunication(communication.id, communicationScope));
      setConfirmingSend(false);
    } catch (error) {
      setCommunicationError(error instanceof Error ? error.message : 'No se pudo enviar la comunicación.');
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="container panel alert-inbox-panel">
      <div className="panel-header alert-inbox-header">
        <div className="alert-inbox-heading">
          <p className="section-label">Bandeja académica</p>

          <div className="alert-inbox-title-line">
            <h3 className="panel-title">Alertas que requieren atención</h3>

            <span className="alert-open-count" aria-live="polite">
              {countCopy(total, attentionState)}
            </span>
          </div>
        </div>

        <div className="alert-inbox-filters">
          <label className="alert-filter" htmlFor="alert-course-filter">
            Curso

            <select
              id="alert-course-filter"
              value={selectedCourseId ?? ''}
              onChange={(event) => onCourseChange(event.target.value || null)}
            >
              <option value="">Todos los cursos</option>

              {courses.map((course) => (
                <option key={course.id} value={course.id}>
                  {course.name}
                  {course.subject ? ` — ${course.subject}` : ''}
                </option>
              ))}
            </select>
          </label>

          <label className="alert-filter" htmlFor="alert-period-filter">
            Período

            <select
              id="alert-period-filter"
              value={selectedAcademicPeriodId ?? ''}
              onChange={(event) =>
                onAcademicPeriodChange(event.target.value || null)
              }
            >
              <option value="">Todos los períodos</option>

              {periods.map((period) => (
                <option key={period.id} value={period.id}>
                  {period.abbreviation ? `${period.abbreviation} — ` : ''}
                  {period.name}
                </option>
              ))}
            </select>
          </label>

          <fieldset className="alert-attention-filter">
            <legend>Estado de atención</legend>

            <div className="alert-attention-options">
              {attentionOptions.map((option) => (
                <button
                  key={option.value}
                  className="alert-attention-option"
                  type="button"
                  aria-pressed={attentionState === option.value}
                  onClick={() => onAttentionStateChange(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </fieldset>
        </div>
      </div>

      {selectedAcademicPeriodId === null && (
        <p className="alert-scope-note">
          Incluye alertas abiertas de períodos anteriores.
        </p>
      )}

      {loading && (
        <p className="alert-refresh-status" role="status">
          {inbox ? 'Actualizando alertas…' : 'Cargando alertas…'}
        </p>
      )}

      {error && (
        <div className="alert-local-error" role="alert">
          <p>{error}</p>

          <button
            className="btn btn-secondary"
            type="button"
            onClick={() => void onRetry()}
          >
            Reintentar
          </button>
        </div>
      )}

      {actionError && (
        <div className="alert-local-error alert-action-error" role="alert">
          <p>{actionError}</p>

          <button
            className="btn btn-secondary"
            type="button"
            onClick={() => void onRetryAction()}
          >
            Reintentar acción
          </button>
        </div>
      )}

      {!loading && !error && inbox && alerts.length === 0 && (
        <p className="alert-empty-state">
          {emptyStateMessage(
            selectedCourseId,
            selectedAcademicPeriodId,
            attentionState,
          )}
        </p>
      )}

      {alerts.length > 0 && (
        <div className="alert-list" aria-label="Alertas abiertas">
          {alerts.map((alert) => (
            <AlertRow
              key={alert.id}
              alert={alert}
              actionPending={actionAlertIds.has(alert.id)}
              onAcknowledge={onAcknowledge}
              onMarkPending={onMarkPending}
              canPrepareCommunication={communicationScope !== null}
              preparingCommunication={preparingAlertId === alert.id}
              onPrepareCommunication={prepareCommunication}
            />
          ))}
        </div>
      )}

      {communication && (
        <section className="alert-communication-editor" role="dialog" aria-label="Comunicación al representante">
          <h4>Comunicación al representante</h4>
          {communicationError && <p role="alert">{communicationError}</p>}
          {communication.status === 'SENT' ? (
            <p role="status">Enviado{communication.sentAt ? ` el ${new Intl.DateTimeFormat('es-EC', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(communication.sentAt))}` : ''}.</p>
          ) : (
            <>
              <label>
                Asunto
                <input value={subject} maxLength={200} onChange={(event) => setSubject(event.target.value)} disabled={sending} />
              </label>
              <label>
                Contenido
                <textarea value={content} maxLength={20000} onChange={(event) => setContent(event.target.value)} disabled={sending} rows={10} />
              </label>
              <div className="alert-communication-actions">
                <button className="btn btn-secondary" type="button" onClick={() => void saveDraft()} disabled={saving || sending}>
                  {saving ? 'Guardando…' : 'Guardar borrador'}
                </button>
                {!confirmingSend ? (
                  <button className="btn btn-primary" type="button" onClick={() => setConfirmingSend(true)} disabled={saving || sending}>
                    Enviar por Idukay
                  </button>
                ) : (
                  <div>
                    <p>Esta comunicación será enviada al representante oficial mediante Idukay.</p>
                    <button className="btn btn-primary" type="button" onClick={() => void confirmSend()} disabled={sending}>
                      {sending ? 'Enviando…' : 'Confirmar envío'}
                    </button>
                    <button className="btn btn-secondary" type="button" onClick={() => setConfirmingSend(false)} disabled={sending}>Cancelar</button>
                  </div>
                )}
              </div>
            </>
          )}
          <button className="btn btn-secondary" type="button" onClick={() => setCommunication(null)} disabled={sending}>Cerrar</button>
        </section>
      )}
    </section>
  );
}

function countCopy(total: number, attentionState: AlertAttentionState) {
  if (attentionState === 'PENDING') {
    return `${total} ${total === 1 ? 'pendiente' : 'pendientes'}`;
  }

  if (attentionState === 'ACKNOWLEDGED') {
    return `${total} ${total === 1 ? 'atendida' : 'atendidas'}`;
  }

  return `${total} ${total === 1 ? 'abierta' : 'abiertas'}`;
}

function emptyStateMessage(
  courseId: string | null,
  academicPeriodId: string | null,
  attentionState: AlertAttentionState,
) {
  const stateLabel =
    attentionState === 'PENDING'
      ? 'pendientes'
      : attentionState === 'ACKNOWLEDGED'
        ? 'atendidas'
        : 'abiertas';

  if (courseId && academicPeriodId) {
    return `No hay alertas ${stateLabel} para este curso y período.`;
  }

  if (courseId) {
    return `No hay alertas ${stateLabel} para este curso.`;
  }

  if (academicPeriodId) {
    return `No hay alertas ${stateLabel} para este período.`;
  }

  return `No hay alertas ${stateLabel}.`;
}

function AlertRow({
  alert,
  actionPending,
  onAcknowledge,
  onMarkPending,
  canPrepareCommunication,
  preparingCommunication,
  onPrepareCommunication,
}: {
  alert: AlertInboxItem;
  actionPending: boolean;
  onAcknowledge: (alertId: string) => void | Promise<void>;
  onMarkPending: (alertId: string) => void | Promise<void>;
  canPrepareCommunication: boolean;
  preparingCommunication: boolean;
  onPrepareCommunication: (alertId: string) => void | Promise<void>;
}) {
  const score = formatAlertScore(alert.score);
  const maximumScore = formatAlertScore(alert.activity.maximumScore);
  const acknowledged = alert.acknowledgedAt !== null;

  return (
    <article className="alert-row">
      <div className="alert-identity">
        <SeverityBadge severity={alert.severity} />

        <h4>{alert.student.name}</h4>
      </div>

      <div className="alert-details">
        <p className="alert-activity-name">{alert.activity.name}</p>

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
            Entrega: {formatAlertDueDate(alert.activity.dueDate)}
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

      <div className="alert-triage-action">
        <p
          className={`alert-attention-state ${
            acknowledged
              ? 'alert-attention-state-acknowledged'
              : 'alert-attention-state-pending'
          }`}
        >
          {acknowledged ? 'Atendida' : 'Pendiente'}
        </p>

        {alert.acknowledgedAt && (
          <p className="alert-acknowledged-date">
            Atendida el {formatAcknowledgedAt(alert.acknowledgedAt)}
          </p>
        )}

        <button
          className="btn btn-secondary alert-triage-button"
          type="button"
          disabled={actionPending}
          onClick={() => {
            if (acknowledged) {
              void onMarkPending(alert.id);
            } else {
              void onAcknowledge(alert.id);
            }
          }}
        >
          {actionPending
            ? 'Actualizando…'
            : acknowledged
              ? 'Marcar como pendiente'
              : 'Marcar como atendida'}
        </button>
        {canPrepareCommunication && (
          <button className="btn btn-secondary alert-triage-button" type="button" disabled={actionPending || preparingCommunication} onClick={() => void onPrepareCommunication(alert.id)}>
            {preparingCommunication ? 'Preparando…' : 'Preparar comunicación'}
          </button>
        )}
      </div>
    </article>
  );
}

function SeverityBadge({ severity }: { severity: AlertSeverity }) {
  const label = severity === 'CRITICAL' ? 'Crítica' : 'Advertencia';

  return (
    <span
      className={`alert-severity alert-severity-${severity.toLowerCase()}`}
    >
      {label}
    </span>
  );
}
