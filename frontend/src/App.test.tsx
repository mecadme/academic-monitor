import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import App from './App';

const context = { institutionId: 'institution-1', teacherUserId: 'teacher-1' };
const periods = { ...context, periods: [
  { id: 'period-1', academicYearId: 'year-1', externalId: 'external-period-1', name: 'Periodo 1', abbreviation: 'P1', order: 1, synchronized: true, academicYear: '2026–2027' },
  { id: 'period-2', academicYearId: 'year-1', externalId: 'external-period-2', name: 'Periodo 2', abbreviation: 'P2', order: 2, synchronized: true, academicYear: '2026–2027' },
] };
const dashboard = { ...context, summary: { courses: 1, students: 24, activities: 5, openAlerts: 2, warnings: 1, critical: 1 }, courses: [{ id: 'course-1', name: '2.º BGU A', subject: 'Física', academicYear: '2026–2027', students: 24, activities: 5, openAlerts: 2, warnings: 1, critical: 1 }] };

describe('App navigation', () => {
  afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, '', '/'); window.localStorage.clear(); });

  it('renders the shell and navigates through the clickable course card', async () => {
    vi.stubGlobal('fetch', fetchForApp()); const user = userEvent.setup(); render(<App />);
    expect(await screen.findByRole('heading', { name: 'Seguimiento académico' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cursos' }));
    const course = await screen.findByRole('link', { name: /2.º BGU A/i });
    expect(course).toHaveAttribute('href', '/courses/course-1');
    await user.click(course);
    expect(await screen.findByRole('tab', { name: 'Alertas' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/courses/course-1');
  });

  it('supports keyboard navigation on a course card', async () => {
    vi.stubGlobal('fetch', fetchForApp()); const user = userEvent.setup(); render(<App />);
    await user.click(await screen.findByRole('button', { name: 'Cursos' }));
    const course = await screen.findByRole('link', { name: /2.º BGU A/i });
    course.focus(); await user.keyboard('{Enter}');
    expect(window.location.pathname).toBe('/courses/course-1');
  });

  it('uses the selected period in the dashboard request', async () => {
    const fetchMock = fetchForApp(); vi.stubGlobal('fetch', fetchMock); const user = userEvent.setup(); render(<App />);
    await screen.findByRole('heading', { name: 'Seguimiento académico' });
    await user.selectOptions(screen.getByLabelText('Período'), 'period-2');
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url).includes('academicPeriodId=period-2'))).toBe(true));
  });

  it('keeps integration settings accessible and does not expose test controls on the dashboard', async () => {
    vi.stubGlobal('fetch', fetchForApp()); const user = userEvent.setup(); render(<App />);
    await screen.findByRole('heading', { name: 'Seguimiento académico' });
    expect(screen.getByRole('heading', { name: 'Estado de datos' })).toBeInTheDocument();
    expect(screen.queryByText(/test-login|test-periods|test-sync/i)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Configuración' }));
    expect(await screen.findByRole('heading', { name: 'Integraciones' })).toBeInTheDocument();
  });

  it('loads a communication detail with academic context and plain-text editing', async () => {
    window.history.replaceState({}, '', '/communications/communication-1');
    vi.stubGlobal('fetch', fetchForApp());
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Comunicación al representante' })).toBeInTheDocument();
    expect(screen.getByText('Xavier Paul Ruilova Soquilli')).toBeInTheDocument();
    expect(screen.getByText('Transformación de unidades')).toBeInTheDocument();
    expect(screen.getByLabelText('Mensaje')).toHaveValue('Estimado/a representante:\n\nLe informamos sobre el progreso académico.');
    expect(screen.getByLabelText('Mensaje')).not.toHaveValue(expect.stringContaining('<p>'));
  });
});

function fetchForApp() { return vi.fn((url: string) => { if (url.includes('/auth/me')) return Promise.resolve(json({ user: { id: 'teacher-1', email: 'teacher@example.com', systemRole: 'USER' }, institution: { id: 'institution-1', name: 'Colegio', role: 'TEACHER' } })); if (url.includes('/academic-periods')) return Promise.resolve(json(periods)); if (url.includes('/dashboard')) return Promise.resolve(json(dashboard)); if (url.includes('/alerts')) return Promise.resolve(json({ ...context, total: 0, alerts: [] })); if (url.includes('/communications/communication-1')) return Promise.resolve(json({ id: 'communication-1', alertId: 'alert-1', studentId: 'student-1', status: 'DRAFT', subject: 'Seguimiento académico', content: '<p>Estimado/a representante:</p><p>Le informamos sobre el progreso académico.</p>', createdAt: '2026-10-03T12:00:00Z', sentAt: null, studentName: 'Xavier Paul Ruilova Soquilli', courseName: '1.º BGU A', courseSubject: 'Física', activityName: 'Transformación de unidades', score: 1, maximumScore: 10, alertSeverity: 'CRITICAL' })); if (url.includes('/communications')) return Promise.resolve(json([])); return Promise.resolve(json({})); }); }
function json(body: unknown) { return { ok: true, json: async () => body } as Response; }
