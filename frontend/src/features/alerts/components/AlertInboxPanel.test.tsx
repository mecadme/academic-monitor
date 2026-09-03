import {
  render,
  screen,
  within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  type ComponentProps,
  useState,
} from 'react';
import {
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import type { AcademicDashboardCourse } from '../../dashboard/api/fetchAcademicDashboard';
import type { AcademicPeriod } from '../api/fetchAcademicPeriods';
import type { AlertInbox } from '../api/fetchAlertInbox';
import {
  formatAcknowledgedAt,
  formatAlertDueDate,
} from '../lib/formatAlert';
import { AlertInboxPanel } from './AlertInboxPanel';

const courses: AcademicDashboardCourse[] = [
  {
    id: 'course-1',
    name: 'Primer Curso A, Bachillerato General Unificado',
    subject: 'Física',
    academicYear: '2025 - 2026',
    students: 30,
    activities: 20,
    openAlerts: 2,
    warnings: 1,
    critical: 1,
  },
  {
    id: 'course-2',
    name: 'Segundo Curso B, Bachillerato General Unificado',
    subject: 'Química',
    academicYear: '2025 - 2026',
    students: 28,
    activities: 18,
    openAlerts: 0,
    warnings: 0,
    critical: 0,
  },
];

const periods: AcademicPeriod[] = [
  {
    id: 'period-t1-internal',
    name: 'Primer trimestre',
    abbreviation: 'T1',
    order: 1,
    synchronized: true,
  },
  {
    id: 'period-t2-internal',
    name: 'Segundo trimestre',
    abbreviation: 'T2',
    order: 2,
    synchronized: false,
  },
];

const populatedInbox: AlertInbox = {
  institutionId: 'institution-internal-id',
  teacherUserId: 'teacher-internal-id',
  total: 2,
  alerts: [
    {
      id: 'alert-critical-id',
      severity: 'CRITICAL',
      ruleCode: 'LOW_GRADE',
      score: 1,
      acknowledgedAt: null,
      course: {
        id: 'course-1',
        name: 'Primer Curso A, Bachillerato General Unificado',
        subject: 'Física',
      },
      activity: {
        id: 'activity-external-id',
        name: 'Transformación de unidades',
        maximumScore: 10,
        dueDate: '2025-11-07',
      },
      student: {
        id: 'student-external-id',
        name: 'Lucía Vega',
      },
    },
    {
      id: 'alert-warning-id',
      severity: 'WARNING',
      ruleCode: 'LOW_GRADE',
      score: 4.5,
      acknowledgedAt: '2026-09-02T18:30:00Z',
      course: {
        id: 'course-2',
        name: 'Segundo Curso B, Bachillerato General Unificado',
        subject: 'Química',
      },
      activity: {
        id: 'activity-2',
        name: 'Enlaces químicos',
        maximumScore: 10,
        dueDate: null,
      },
      student: {
        id: 'student-2',
        name: 'Ana Torres',
      },
    },
  ],
};

describe('AlertInboxPanel', () => {
  it('renders count, severity labels, enrichment, scores and backend order', () => {
    renderPanel({ inbox: populatedInbox });

    expect(screen.getByText('2 abiertas')).toBeInTheDocument();
    expect(screen.getByText('Crítica')).toBeInTheDocument();
    expect(screen.getByText('Advertencia')).toBeInTheDocument();
    expect(
      screen.getByText('Lucía Vega'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Transformación de unidades'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Primer Curso A, Bachillerato General Unificado',
      ),
    ).toBeInTheDocument();
    expect(screen.getByText('Física')).toBeInTheDocument();
    expect(
      screen.getByLabelText('Calificación 1.00 de 10.00'),
    ).toHaveTextContent('1.00 / 10.00');

    const rows = screen.getAllByRole('article');
    expect(
      within(rows[0]).getByText(
        'Lucía Vega',
      ),
    ).toBeInTheDocument();
    expect(
      within(rows[1]).getByText('Ana Torres'),
    ).toBeInTheDocument();
  });

  it('formats a due date with Intl and omits it when absent', () => {
    renderPanel({ inbox: populatedInbox });

    expect(
      screen.getByText(
        `Entrega: ${formatAlertDueDate('2025-11-07')}`,
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/^Entrega:/)).toHaveLength(1);
  });

  it('uses dashboard courses for the filter and emits null for all courses', async () => {
    const user = userEvent.setup();
    const onCourseChange = vi.fn();

    function FilterHarness() {
      const [selectedCourseId, setSelectedCourseId] =
        useState<string | null>(null);

      return (
        <AlertInboxPanel
          {...defaultProps}
          selectedCourseId={selectedCourseId}
          onCourseChange={(courseId) => {
            setSelectedCourseId(courseId);
            onCourseChange(courseId);
          }}
        />
      );
    }

    render(<FilterHarness />);

    const filter = screen.getByLabelText('Curso');
    expect(
      screen.getByRole('option', {
        name: 'Primer Curso A, Bachillerato General Unificado — Física',
      }),
    ).toBeInTheDocument();

    await user.selectOptions(filter, 'course-1');
    expect(onCourseChange).toHaveBeenLastCalledWith('course-1');

    await user.selectOptions(filter, '');
    expect(onCourseChange).toHaveBeenLastCalledWith(null);
  });

  it('uses internal academic period IDs and emits null for all periods', async () => {
    const user = userEvent.setup();
    const onAcademicPeriodChange = vi.fn();

    function FilterHarness() {
      const [selectedAcademicPeriodId, setSelectedAcademicPeriodId] =
        useState<string | null>('period-t2-internal');

      return (
        <AlertInboxPanel
          {...defaultProps}
          selectedAcademicPeriodId={selectedAcademicPeriodId}
          onAcademicPeriodChange={(academicPeriodId) => {
            setSelectedAcademicPeriodId(academicPeriodId);
            onAcademicPeriodChange(academicPeriodId);
          }}
        />
      );
    }

    render(<FilterHarness />);

    const filter = screen.getByLabelText('Período');
    expect(filter).toHaveValue('period-t2-internal');
    expect(
      screen.getByRole('option', {
        name: 'T1 — Primer trimestre',
      }),
    ).toBeInTheDocument();

    await user.selectOptions(filter, 'period-t1-internal');
    expect(onAcademicPeriodChange).toHaveBeenLastCalledWith(
      'period-t1-internal',
    );

    await user.selectOptions(filter, '');
    expect(onAcademicPeriodChange).toHaveBeenLastCalledWith(null);
  });

  it('renders positive empty states for all courses and a selected course', () => {
    const emptyInbox = {
      ...populatedInbox,
      total: 0,
      alerts: [],
    };
    const { rerender } = renderPanel({
      inbox: emptyInbox,
    });

    expect(
      screen.getByText('No hay alertas abiertas.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Incluye alertas abiertas de períodos anteriores.',
      ),
    ).toBeInTheDocument();

    rerender(
      <AlertInboxPanel
        {...defaultProps}
        inbox={emptyInbox}
        selectedCourseId="course-1"
      />,
    );

    expect(
      screen.getByText(
        'No hay alertas abiertas para este curso.',
      ),
    ).toBeInTheDocument();

    rerender(
      <AlertInboxPanel
        {...defaultProps}
        inbox={emptyInbox}
        selectedAcademicPeriodId="period-t1-internal"
      />,
    );

    expect(
      screen.getByText(
        'No hay alertas abiertas para este período.',
      ),
    ).toBeInTheDocument();
  });

  it('shows a local error with a semantic retry button', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();

    renderPanel({
      inbox: null,
      error: 'No se pudieron cargar las alertas.',
      onRetry,
    });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'No se pudieron cargar las alertas.',
    );

    await user.click(
      screen.getByRole('button', {
        name: 'Reintentar',
      }),
    );
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('shows initial loading and preserves rows while refreshing', () => {
    const { rerender } = renderPanel({
      inbox: null,
      loading: true,
    });

    expect(screen.getByRole('status')).toHaveTextContent(
      'Cargando alertas…',
    );

    rerender(
      <AlertInboxPanel
        {...defaultProps}
        inbox={populatedInbox}
        loading
      />,
    );

    expect(screen.getByRole('status')).toHaveTextContent(
      'Actualizando alertas…',
    );
    expect(
      screen.getByText('Lucía Vega'),
    ).toBeInTheDocument();
  });

  it('does not render backend or platform identifiers', () => {
    renderPanel({ inbox: populatedInbox });

    expect(
      screen.queryByText('institution-internal-id'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText('teacher-internal-id'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText('student-external-id'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText('activity-external-id'),
    ).not.toBeInTheDocument();
    expect(screen.queryByText('LOW_GRADE')).not.toBeInTheDocument();
    expect(screen.queryByText(/idukay/i)).not.toBeInTheDocument();
  });

  it('renders the active triage count and exposes all attention filters', async () => {
    const user = userEvent.setup();
    const onAttentionStateChange = vi.fn();

    renderPanel({
      inbox: populatedInbox,
      attentionState: 'PENDING',
      onAttentionStateChange,
    });

    expect(screen.getByText('2 pendientes')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Pendientes' }),
    ).toHaveAttribute('aria-pressed', 'true');

    await user.click(
      screen.getByRole('button', { name: 'Atendidas' }),
    );
    expect(onAttentionStateChange).toHaveBeenCalledWith('ACKNOWLEDGED');

    await user.click(
      screen.getByRole('button', { name: 'Todas activas' }),
    );
    expect(onAttentionStateChange).toHaveBeenCalledWith('ALL');
  });

  it('shows attention state, acknowledgement time and the matching row actions', async () => {
    const user = userEvent.setup();
    const onAcknowledge = vi.fn();
    const onMarkPending = vi.fn();

    renderPanel({ onAcknowledge, onMarkPending });

    const rows = screen.getAllByRole('article');
    expect(within(rows[0]).getByText('Pendiente')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Atendida')).toBeInTheDocument();
    expect(within(rows[1]).getByText(
      `Atendida el ${formatAcknowledgedAt('2026-09-02T18:30:00Z')}`,
    )).toBeInTheDocument();

    await user.click(
      within(rows[0]).getByRole('button', {
        name: 'Marcar como atendida',
      }),
    );
    expect(onAcknowledge).toHaveBeenCalledWith('alert-critical-id');

    await user.click(
      within(rows[1]).getByRole('button', {
        name: 'Marcar como pendiente',
      }),
    );
    expect(onMarkPending).toHaveBeenCalledWith('alert-warning-id');
  });

  it('disables only the alert whose action is in flight', () => {
    renderPanel({ actionAlertIds: new Set(['alert-critical-id']) });

    const rows = screen.getAllByRole('article');
    expect(
      within(rows[0]).getByRole('button', { name: 'Actualizando…' }),
    ).toBeDisabled();
    expect(
      within(rows[1]).getByRole('button', {
        name: 'Marcar como pendiente',
      }),
    ).toBeEnabled();
  });

  it('keeps rows visible when an action fails and provides a local retry', async () => {
    const user = userEvent.setup();
    const onRetryAction = vi.fn();

    renderPanel({
      actionError: 'No se pudo marcar la alerta como atendida.',
      onRetryAction,
    });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'No se pudo marcar la alerta como atendida.',
    );
    expect(screen.getAllByRole('article')).toHaveLength(2);

    await user.click(
      screen.getByRole('button', { name: 'Reintentar acción' }),
    );
    expect(onRetryAction).toHaveBeenCalledTimes(1);
  });
});

const defaultProps = {
  courses,
  periods,
  inbox: populatedInbox,
  loading: false,
  error: null,
  actionError: null,
  actionAlertIds: new Set<string>(),
  selectedCourseId: null,
  selectedAcademicPeriodId: null,
  attentionState: 'ALL' as const,
  onCourseChange: vi.fn(),
  onAcademicPeriodChange: vi.fn(),
  onAttentionStateChange: vi.fn(),
  onRetry: vi.fn(),
  onRetryAction: vi.fn(),
  onAcknowledge: vi.fn(),
  onMarkPending: vi.fn(),
};

function renderPanel(
  overrides: Partial<ComponentProps<typeof AlertInboxPanel>> = {},
) {
  return render(
    <AlertInboxPanel
      {...defaultProps}
      {...overrides}
    />,
  );
}
