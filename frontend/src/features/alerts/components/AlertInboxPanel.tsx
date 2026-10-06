import { useState } from 'react';
import type { AcademicDashboardCourse } from '../../dashboard/api/fetchAcademicDashboard';
import type { AcademicPeriod } from '../api/fetchAcademicPeriods';
import type { AlertAttentionState, AlertInbox, AlertInboxItem, AlertSeverity } from '../api/fetchAlertInbox';
import { prepareAlertCommunication } from '../api/communications';
import { formatAcknowledgedAt, formatAlertDueDate, formatAlertScore } from '../lib/formatAlert';

type Props = {
  courses: AcademicDashboardCourse[]; periods: AcademicPeriod[]; inbox: AlertInbox | null; loading: boolean; error: string | null;
  actionError: string | null; actionAlertIds: ReadonlySet<string>; selectedCourseId: string | null; selectedAcademicPeriodId: string | null;
  attentionState: AlertAttentionState; onCourseChange: (value: string | null) => void; onAcademicPeriodChange: (value: string | null) => void;
  onAttentionStateChange: (value: AlertAttentionState) => void; onRetry: () => void | Promise<void>; onRetryAction: () => void | Promise<void>;
  onAcknowledge: (id: string) => void | Promise<void>; onMarkPending: (id: string) => void | Promise<void>; showCourseFilter?: boolean; showPeriodFilter?: boolean; onCommunicationNavigate?: (id: string) => void;
};
const states: Array<[AlertAttentionState, string]> = [['PENDING', 'Pendientes'], ['ACKNOWLEDGED', 'Atendidas'], ['ALL', 'Todas activas']];

export function AlertInboxPanel(props: Props) {
  const [preparing, setPreparing] = useState<string | null>(null);
  const [communicationError, setCommunicationError] = useState<string | null>(null);
  const prepare = async (alertId: string) => {
    if (preparing) return;
    setPreparing(alertId); setCommunicationError(null);
    try { const communication = await prepareAlertCommunication(alertId); props.onCommunicationNavigate?.(communication.id); }
    catch (error) { setCommunicationError(error instanceof Error ? error.message : 'No se pudo preparar el borrador.'); }
    finally { setPreparing(null); }
  };
  const alerts = props.inbox?.alerts ?? [];
  return <section className="container panel alert-inbox-panel">
    <div className="panel-header alert-inbox-header"><div className="alert-inbox-heading"><p className="section-label">Bandeja académica</p><div className="alert-inbox-title-line"><h3 className="panel-title">Alertas que requieren atención</h3><span className="alert-open-count">{countCopy(props.inbox?.total ?? 0, props.attentionState)}</span></div></div>
      <div className="alert-inbox-filters">
        {props.showCourseFilter !== false && <label className="alert-filter" htmlFor="alert-course-filter">Curso<select id="alert-course-filter" value={props.selectedCourseId ?? ''} onChange={(event) => props.onCourseChange(event.target.value || null)}><option value="">Todos los cursos</option>{props.courses.map((course) => <option key={course.id} value={course.id}>{course.name}{course.subject ? ' — ' + course.subject : ''}</option>)}</select></label>}
        {props.showPeriodFilter !== false && <label className="alert-filter" htmlFor="alert-period-filter">Período<select id="alert-period-filter" value={props.selectedAcademicPeriodId ?? ''} onChange={(event) => props.onAcademicPeriodChange(event.target.value || null)}><option value="">Todos los períodos</option>{props.periods.map((period) => <option key={period.id} value={period.id}>{(period.abbreviation ? period.abbreviation + ' — ' : '') + period.name}</option>)}</select></label>}
        <fieldset className="alert-attention-filter"><legend>Estado de atención</legend><div className="alert-attention-options">{states.map(([value, label]) => <button key={value} className="alert-attention-option" type="button" aria-pressed={props.attentionState === value} onClick={() => props.onAttentionStateChange(value)}>{label}</button>)}</div></fieldset>
      </div>
    </div>
    {props.loading && <p className="alert-refresh-status" role="status">{props.inbox ? 'Actualizando alertas…' : 'Cargando alertas…'}</p>}
    {(props.error || communicationError) && <div className="alert-local-error" role="alert"><p>{props.error ?? communicationError}</p>{props.error && <button className="btn btn-secondary" type="button" onClick={() => void props.onRetry()}>Reintentar</button>}</div>}
    {props.actionError && <div className="alert-local-error" role="alert"><p>{props.actionError}</p><button className="btn btn-secondary" type="button" onClick={() => void props.onRetryAction()}>Reintentar acción</button></div>}
    {!props.loading && !props.error && props.inbox && alerts.length === 0 && <p className="alert-empty-state">{emptyCopy(props.selectedCourseId, props.selectedAcademicPeriodId, props.attentionState)}</p>}
    {alerts.length > 0 && <div className="alert-list" aria-label="Alertas abiertas">{alerts.map((alert) => <AlertRow key={alert.id} alert={alert} actionPending={props.actionAlertIds.has(alert.id)} canCommunicate={!!props.onCommunicationNavigate} preparing={preparing === alert.id} onAcknowledge={props.onAcknowledge} onMarkPending={props.onMarkPending} onPrepare={prepare} onNavigate={props.onCommunicationNavigate} />)}</div>}
  </section>;
}

function AlertRow({ alert, actionPending, canCommunicate, preparing, onAcknowledge, onMarkPending, onPrepare, onNavigate }: { alert: AlertInboxItem; actionPending: boolean; canCommunicate: boolean; preparing: boolean; onAcknowledge: (id: string) => void | Promise<void>; onMarkPending: (id: string) => void | Promise<void>; onPrepare: (id: string) => void | Promise<void>; onNavigate?: (id: string) => void }) {
  const acknowledged = alert.acknowledgedAt !== null;
  const communication = alert.communication;
  return <article className="alert-row"><div className="alert-identity"><SeverityBadge severity={alert.severity} /><h4>{alert.student.name}</h4></div><div className="alert-details"><p className="alert-activity-name">{alert.activity.name}</p><p className="alert-course-name">{alert.course.name}{alert.course.subject ? ' · ' + alert.course.subject : ''}</p>{alert.activity.dueDate && <p className="alert-due-date">Entrega: {formatAlertDueDate(alert.activity.dueDate)}</p>}</div><p className="alert-score" aria-label={'Calificación ' + formatAlertScore(alert.score) + ' de ' + formatAlertScore(alert.activity.maximumScore)}><strong>{formatAlertScore(alert.score)}</strong><span> / {formatAlertScore(alert.activity.maximumScore)}</span></p><div className="alert-triage-action"><p className={'alert-attention-state ' + (acknowledged ? 'alert-attention-state-acknowledged' : 'alert-attention-state-pending')}>{acknowledged ? 'Atendida' : 'Pendiente'}</p>{alert.acknowledgedAt && <p className="alert-acknowledged-date">Atendida el {formatAcknowledgedAt(alert.acknowledgedAt)}</p>}<button className="btn btn-secondary alert-triage-button" type="button" disabled={actionPending} onClick={() => void (acknowledged ? onMarkPending(alert.id) : onAcknowledge(alert.id))}>{actionPending ? 'Actualizando…' : acknowledged ? 'Marcar como pendiente' : 'Marcar como atendida'}</button>{canCommunicate && <CommunicationAction communication={communication} preparing={preparing} alertId={alert.id} onPrepare={onPrepare} onNavigate={onNavigate} />}</div></article>;
}
function CommunicationAction({ communication, preparing, alertId, onPrepare, onNavigate }: { communication: AlertInboxItem['communication']; preparing: boolean; alertId: string; onPrepare: (id: string) => void | Promise<void>; onNavigate?: (id: string) => void }) {
  if (!communication) return <button className="btn btn-secondary alert-triage-button" type="button" disabled={preparing} onClick={() => void onPrepare(alertId)}>{preparing ? 'Preparando…' : 'Preparar comunicación'}</button>;
  if (communication.status === 'PENDING') return <button className="btn btn-secondary alert-triage-button" type="button" disabled>Enviando…</button>;
  const labels = { DRAFT: 'Continuar borrador', SENT: 'Ver comunicación', FAILED: 'Revisar comunicación', PENDING: null };
  const status = { DRAFT: 'Borrador', SENT: 'Enviada', FAILED: 'No se pudo enviar', PENDING: 'Enviando…' };
  return <><span className={'communication-status communication-status-' + communication.status.toLowerCase()}>{status[communication.status]}</span>{labels[communication.status] && <button className="btn btn-secondary alert-triage-button" type="button" onClick={() => onNavigate?.(communication.id)}>{labels[communication.status]}</button>}</>;
}
function SeverityBadge({ severity }: { severity: AlertSeverity }) { return <span className={'alert-severity alert-severity-' + severity.toLowerCase()}>{severity === 'CRITICAL' ? 'Crítica' : 'Advertencia'}</span>; }
function countCopy(total: number, state: AlertAttentionState) { const label = state === 'PENDING' ? 'pendiente' : state === 'ACKNOWLEDGED' ? 'atendida' : 'abierta'; return String(total) + ' ' + (total === 1 ? label : label + 's'); }
function emptyCopy(courseId: string | null, periodId: string | null, state: AlertAttentionState) { const label = state === 'PENDING' ? 'pendientes' : state === 'ACKNOWLEDGED' ? 'atendidas' : 'abiertas'; if (courseId && periodId) return 'No hay alertas ' + label + ' para este curso y período.'; if (courseId) return 'No hay alertas ' + label + ' para este curso.'; if (periodId) return 'No hay alertas ' + label + ' para este período.'; return 'No hay alertas ' + label + '.'; }
