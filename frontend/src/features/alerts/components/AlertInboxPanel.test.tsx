import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { AcademicDashboardCourse } from '../../dashboard/api/fetchAcademicDashboard';
import type { AcademicPeriod } from '../api/fetchAcademicPeriods';
import type { AlertInbox, AlertInboxItem } from '../api/fetchAlertInbox';
import { AlertInboxPanel } from './AlertInboxPanel';

const courses: AcademicDashboardCourse[] = [{ id: 'course-1', name: '1.º BGU A', subject: 'Física', academicYear: '2026-2027', students: 30, activities: 8, openAlerts: 5, warnings: 3, critical: 2 }];
const periods: AcademicPeriod[] = [{ id: 'period-1', academicYearId: 'year-1', externalId: 'external-period-1', name: 'Periodo 1', abbreviation: 'P1', order: 1, synchronized: true }];
const baseAlert: Omit<AlertInboxItem, 'id' | 'communication'> = { severity: 'CRITICAL', ruleCode: 'LOW_GRADE', score: 1, acknowledgedAt: null, course: { id: 'course-1', name: '1.º BGU A', subject: 'Física' }, activity: { id: 'activity-1', name: 'Transformación de unidades', maximumScore: 10, dueDate: null }, student: { id: 'student-1', name: 'Xavier Paul Ruilova Soquilli' } };

function alert(id: string, communication: AlertInboxItem['communication']): AlertInboxItem { return { ...baseAlert, id, communication }; }
function inbox(alerts: AlertInboxItem[]): AlertInbox { return { institutionId: 'institution-id', teacherUserId: 'teacher-id', total: alerts.length, alerts }; }

const defaults = {
  courses, periods, inbox: inbox([alert('alert-1', null)]), loading: false, error: null, actionError: null, actionAlertIds: new Set<string>(), selectedCourseId: null, selectedAcademicPeriodId: 'period-1', attentionState: 'PENDING' as const,
  onCourseChange: vi.fn(), onAcademicPeriodChange: vi.fn(), onAttentionStateChange: vi.fn(), onRetry: vi.fn(), onRetryAction: vi.fn(), onAcknowledge: vi.fn(), onMarkPending: vi.fn(), institutionId: 'institution-id', teacherUserId: 'teacher-id', onCommunicationNavigate: vi.fn(),
};

describe('AlertInboxPanel', () => {
  it('renders the real academic alert context without technical identifiers', () => {
    render(<AlertInboxPanel {...defaults} />);
    expect(screen.getByText('Xavier Paul Ruilova Soquilli')).toBeInTheDocument();
    expect(screen.getByText('Transformación de unidades')).toBeInTheDocument();
    expect(screen.getByLabelText('Calificación 1.00 de 10.00')).toBeInTheDocument();
    expect(screen.queryByText('institution-id')).not.toBeInTheDocument();
    expect(screen.queryByText('LOW_GRADE')).not.toBeInTheDocument();
  });

  it('shows the contextual communication action for every workflow state', () => {
    const onCommunicationNavigate = vi.fn();
    render(<AlertInboxPanel {...defaults} onCommunicationNavigate={onCommunicationNavigate} inbox={inbox([
      alert('none', null),
      alert('draft', { id: 'draft-id', status: 'DRAFT' }),
      alert('pending', { id: 'pending-id', status: 'PENDING' }),
      alert('sent', { id: 'sent-id', status: 'SENT' }),
      alert('failed', { id: 'failed-id', status: 'FAILED' }),
    ])} />);
    expect(screen.getByRole('button', { name: 'Preparar comunicación' })).toBeInTheDocument();
    expect(screen.getByText('Borrador')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continuar borrador' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enviando…' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Enviar' })).not.toBeInTheDocument();
    expect(screen.getByText('Enviada')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver comunicación' })).toBeInTheDocument();
    expect(screen.getByText('No se pudo enviar')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Revisar comunicación' })).toBeInTheDocument();
  });

  it('navigates to an existing communication instead of preparing another one', async () => {
    const user = userEvent.setup();
    const onCommunicationNavigate = vi.fn();
    render(<AlertInboxPanel {...defaults} onCommunicationNavigate={onCommunicationNavigate} inbox={inbox([alert('draft', { id: 'draft-id', status: 'DRAFT' })])} />);
    await user.click(screen.getByRole('button', { name: 'Continuar borrador' }));
    expect(onCommunicationNavigate).toHaveBeenCalledWith('draft-id');
  });

  it('prepares only when there is no communication, then navigates to its detail route', async () => {
    const user = userEvent.setup();
    const onCommunicationNavigate = vi.fn();
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 'communication-id', status: 'DRAFT' }));
    vi.stubGlobal('fetch', fetchMock);
    render(<AlertInboxPanel {...defaults} onCommunicationNavigate={onCommunicationNavigate} />);
    await user.click(screen.getByRole('button', { name: 'Preparar comunicación' }));
    await waitFor(() => expect(onCommunicationNavigate).toHaveBeenCalledWith('communication-id'));
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/v1/alerts/alert-1/communication'), expect.objectContaining({ method: 'POST' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    vi.unstubAllGlobals();
  });

  it('keeps alert triage available', async () => {
    const user = userEvent.setup();
    const onAcknowledge = vi.fn();
    render(<AlertInboxPanel {...defaults} onAcknowledge={onAcknowledge} />);
    const row = screen.getByRole('article');
    await user.click(within(row).getByRole('button', { name: 'Marcar como atendida' }));
    expect(onAcknowledge).toHaveBeenCalledWith('alert-1');
  });
});

function jsonResponse(body: unknown) { return { ok: true, json: async () => body } as Response; }
