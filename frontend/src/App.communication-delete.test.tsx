import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import App from './App';
import { type Communication, communicationsRefreshEvent } from './features/alerts/api/communications';
import { useAlertInbox } from './features/alerts/hooks/useAlertInbox';
import { renderHook } from '@testing-library/react';

const scope = { institutionId: 'institution-1', teacherUserId: 'teacher-1' };
const draft: Communication = {
  id: 'communication-1', alertId: 'alert-1', studentId: 'student-1', status: 'DRAFT',
  subject: 'Seguimiento', content: '<p>Mensaje</p>', createdAt: '2026-10-03T12:00:00Z',
  sentAt: null, studentName: 'Estudiante', courseName: 'Curso', courseSubject: 'Física',
  activityName: 'Evaluación', score: 4, maximumScore: 10, alertSeverity: 'CRITICAL',
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', '/');
  window.localStorage.clear();
});

function setup(status: Communication['status'] = 'DRAFT', deleteResponse?: Promise<Response>, options: { list?: boolean; extraDraft?: boolean; refreshResponse?: Promise<Response>; deleteError?: Error } = {}) {
  let deleted = false;
  let listRequests = 0;
  const fetchMock = vi.fn((url: string, init?: RequestInit): Promise<Response> => {
    if (init?.method === 'DELETE') {
      if (options.deleteError) return Promise.reject(options.deleteError);
      if (deleteResponse) return deleteResponse.then((response) => { if (response.ok) deleted = true; return response; });
      deleted = true;
      return Promise.resolve({ ok: true, status: 204 } as Response);
    }
    if (url.includes('/auth/me')) return Promise.resolve(json({ user: { id: 'teacher-1', email: 'teacher@example.com', systemRole: 'USER' }, institution: { id: 'institution-1', name: 'Colegio', role: 'TEACHER' } }));
    if (url.includes('/academic-periods')) return Promise.resolve(json({ ...scope, periods: [{ id: 'period-1', academicYearId: 'year-1', academicYear: '2026-2027', externalId: 'external-period-1', abbreviation: 'P1', name: 'Período 1', order: 1, synchronized: true }] }));
    if (url.includes('/dashboard')) return Promise.resolve(json({ ...scope, summary: {}, courses: [] }));
    if (url.includes('/communications/communication-1')) return Promise.resolve(json({ ...draft, status }));
    if (url.includes('/communications')) {
      listRequests++;
      if (listRequests > 1 && options.refreshResponse) return options.refreshResponse;
      return Promise.resolve(json([
        ...deleted ? [] : [{ ...draft, status }],
        ...options.extraDraft ? [{ ...draft, id: 'communication-2', subject: 'Otro borrador' }] : [],
      ]));
    }
    if (url.includes('/alerts')) return Promise.resolve(json({ ...scope, total: 1, alerts: [{
      id: 'alert-1', severity: 'CRITICAL', ruleCode: 'LOW_GRADE', score: 4, acknowledgedAt: null,
      course: { id: 'course-1', name: 'Curso', subject: 'Física' },
      activity: { id: 'activity-1', name: 'Evaluación', maximumScore: 10, dueDate: null },
      student: { id: 'student-1', name: 'Estudiante' },
      communication: deleted ? null : { id: 'communication-1', status },
    }] }));
    return Promise.resolve(json({ notifications: [], unreadCount: 0 }));
  });
  vi.stubGlobal('fetch', fetchMock);
  window.history.replaceState({}, '', options.list ? '/communications' : '/communications/communication-1');
  render(<App />);
  return fetchMock;
}

async function openConfirmation() {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'Eliminar borrador' }));
  return { user, dialog: screen.getByRole('dialog', { name: '¿Eliminar este borrador?' }) };
}

describe('delete communication draft', () => {
  it('shows a secondary destructive action only for a draft', async () => {
    setup();
    expect(await screen.findByRole('button', { name: 'Eliminar borrador' })).toHaveClass('btn-danger-secondary');
    expect(screen.getByRole('button', { name: 'Guardar borrador' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Enviar por Idukay' })).toBeEnabled();
  });

  it.each(['SENT', 'PENDING', 'FAILED'] as const)('does not show delete for %s', async (status) => {
    setup(status);
    await screen.findByRole('heading', { name: 'Comunicación al representante' });
    expect(screen.queryByRole('button', { name: 'Eliminar borrador' })).not.toBeInTheDocument();
  });

  it('opens an accessible confirmation and cancel does not delete', async () => {
    const fetchMock = setup();
    const { user, dialog } = await openConfirmation();
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAccessibleDescription('Esta comunicación no ha sido enviada y se eliminará de forma permanente.');
    expect(within(dialog).getByRole('button', { name: 'Cancelar' })).toHaveFocus();
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Eliminar borrador' })).toHaveFocus();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false);
  });

  it('traps keyboard focus and closes with Escape', async () => {
    setup();
    const { user, dialog } = await openConfirmation();
    await user.tab({ shift: true });
    expect(within(dialog).getByRole('button', { name: 'Eliminar borrador' })).toHaveFocus();
    await user.tab();
    expect(within(dialog).getByRole('button', { name: 'Cancelar' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('confirms DELETE without frontend scope, invalidates and returns to a refreshed inbox', async () => {
    const fetchMock = setup();
    const refresh = vi.fn();
    window.addEventListener(communicationsRefreshEvent, refresh);
    const { user, dialog } = await openConfirmation();
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    await waitFor(() => expect(window.location.pathname).toBe('/alerts'));
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/v1/communications/communication-1', {
      method: 'DELETE', credentials: 'include', headers: { accept: 'application/json', 'x-xsrf-token': 'test-csrf' },
    });
    expect(refresh).toHaveBeenCalledOnce();
    window.removeEventListener(communicationsRefreshEvent, refresh);
    expect(await screen.findByRole('button', { name: 'Preparar comunicación' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Continuar borrador' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Comunicación al representante' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Comunicaciones' }));
    expect(await screen.findByRole('heading', { name: 'No hay comunicaciones en esta sección' })).toBeInTheDocument();
  });

  it.each([409, 500])('keeps the detail and confirmation open on error %s', async (status) => {
    setup('DRAFT', Promise.resolve({ ok: false, status } as Response));
    const refresh = vi.fn();
    window.addEventListener(communicationsRefreshEvent, refresh);
    const { user, dialog } = await openConfirmation();
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(status === 409 ? 'ya no se puede eliminar' : 'No se pudo eliminar');
    expect(window.location.pathname).toBe('/communications/communication-1');
    expect(within(dialog).getByRole('button', { name: 'Eliminar borrador' })).toBeEnabled();
    expect(refresh).not.toHaveBeenCalled();
    window.removeEventListener(communicationsRefreshEvent, refresh);
  });

  it('disables confirmation and editor actions during delete and submits once', async () => {
    let finish!: (response: Response) => void;
    const response = new Promise<Response>((resolve) => { finish = resolve; });
    const fetchMock = setup('DRAFT', response);
    const { user, dialog } = await openConfirmation();
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(within(dialog).getByRole('button', { name: 'Eliminando…' })).toBeDisabled();
    expect(within(dialog).getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    expect(screen.getByLabelText('Asunto')).toBeDisabled();
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    await user.keyboard('{Escape}');
    expect(dialog).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE')).toHaveLength(1);
    await act(async () => { finish({ ok: true, status: 204 } as Response); });
    await waitFor(() => expect(window.location.pathname).toBe('/alerts'));
  });

  it('refreshes an already mounted alert inbox on successful deletion invalidation', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ ...scope, alerts: [], total: 0 }));
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() => useAlertInbox(scope));
    await waitFor(() => expect(result.current.inbox).not.toBeNull());
    await act(async () => { window.dispatchEvent(new Event(communicationsRefreshEvent)); });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  });
});

describe('delete draft from communications list', () => {
  it('shows both card actions for DRAFT with a secondary destructive button', async () => {
    setup('DRAFT', undefined, { list: true });
    const remove = await screen.findByRole('button', { name: 'Eliminar borrador' });
    expect(remove).toHaveClass('btn-danger-secondary');
    expect(screen.getByRole('button', { name: 'Continuar borrador' })).toBeEnabled();
    expect(remove.closest('.communication-card-actions')).toContainElement(screen.getByRole('button', { name: 'Continuar borrador' }));
  });

  it.each(['SENT', 'PENDING', 'FAILED'] as const)('does not show card delete for %s', async (status) => {
    setup(status, undefined, { list: true });
    await screen.findByRole('heading', { name: 'Seguimiento' });
    expect(screen.queryByRole('button', { name: 'Eliminar borrador' })).not.toBeInTheDocument();
  });

  it('opens the shared accessible confirmation by keyboard without navigating and cancel does not DELETE', async () => {
    const fetchMock = setup('DRAFT', undefined, { list: true });
    const remove = await screen.findByRole('button', { name: 'Eliminar borrador' });
    remove.focus();
    const user = userEvent.setup();
    await user.keyboard('{Enter}');
    const dialog = screen.getByRole('dialog', { name: '¿Eliminar este borrador?' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAccessibleDescription('Esta comunicación no ha sido enviada y se eliminará de forma permanente.');
    expect(within(dialog).getByRole('button', { name: 'Cancelar' })).toHaveFocus();
    expect(window.location.pathname).toBe('/communications');
    expect(fetchMock.mock.calls.some(([url]) => url.includes('/communications/communication-1'))).toBe(false);
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(remove).toHaveFocus();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false);
  });

  it('click opens confirmation without opening the detail', async () => {
    setup('DRAFT', undefined, { list: true });
    await openConfirmation();
    expect(window.location.pathname).toBe('/communications');
    expect(screen.queryByRole('heading', { name: 'Comunicación al representante' })).not.toBeInTheDocument();
  });

  it('deletes the correct card, refetches communications and inbox, stays on the list and shows feedback', async () => {
    const fetchMock = setup('DRAFT', undefined, { list: true, extraDraft: true });
    const refresh = vi.fn();
    window.addEventListener(communicationsRefreshEvent, refresh);
    const { result } = renderHook(() => useAlertInbox(scope));
    await waitFor(() => expect(result.current.inbox?.alerts[0]?.communication?.id).toBe('communication-1'));
    const user = userEvent.setup();
    const card = (await screen.findByRole('heading', { name: 'Seguimiento' })).closest('article')!;
    await user.click(within(card).getByRole('button', { name: 'Eliminar borrador' }));
    const dialog = screen.getByRole('dialog', { name: '¿Eliminar este borrador?' });
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/v1/communications/communication-1', {
      method: 'DELETE', credentials: 'include', headers: { accept: 'application/json', 'x-xsrf-token': 'test-csrf' },
    });
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Seguimiento' })).not.toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Otro borrador' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Borrador eliminado.');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(window.location.pathname).toBe('/communications');
    expect(refresh).toHaveBeenCalledOnce();
    expect(fetchMock.mock.calls.filter(([url]) => new URL(url).pathname === '/api/v1/communications')).toHaveLength(2);
    await waitFor(() => expect(result.current.inbox?.alerts[0]?.communication).toBeNull());
    window.removeEventListener(communicationsRefreshEvent, refresh);
    await user.click(screen.getByRole('button', { name: 'Alertas' }));
    expect(await screen.findByRole('button', { name: 'Preparar comunicación' })).toBeInTheDocument();
  });

  it.each([409, 500])('keeps the card and friendly dialog error on DELETE failure %s', async (status) => {
    const fetchMock = setup('DRAFT', Promise.resolve({ ok: false, status } as Response), { list: true });
    const { user, dialog } = await openConfirmation();
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se pudo eliminar el borrador. Inténtalo nuevamente.');
    expect(screen.getByRole('heading', { name: 'Seguimiento' })).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Eliminar borrador' })).toBeEnabled();
    expect(window.location.pathname).toBe('/communications');
    expect(fetchMock.mock.calls.filter(([url]) => new URL(url).pathname === '/api/v1/communications')).toHaveLength(1);
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(screen.getByRole('button', { name: 'Continuar borrador' })).toBeEnabled();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows a friendly retry message for network errors without exposing the technical error', async () => {
    setup('DRAFT', undefined, { list: true, deleteError: new TypeError('Failed to fetch') });
    const { user, dialog } = await openConfirmation();
    await user.click(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('No se pudo eliminar el borrador. Inténtalo nuevamente.');
    expect(screen.queryByText('Failed to fetch')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Seguimiento' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/communications');
  });

  it('disables only the selected card actions during DELETE, keeps other cards and prevents double submission', async () => {
    let finish!: (response: Response) => void;
    const response = new Promise<Response>((resolve) => { finish = resolve; });
    const fetchMock = setup('DRAFT', response, { list: true, extraDraft: true });
    const user = userEvent.setup();
    const card = (await screen.findByRole('heading', { name: 'Seguimiento' })).closest('article')!;
    const otherCard = screen.getByRole('heading', { name: 'Otro borrador' }).closest('article')!;
    await user.click(within(card).getByRole('button', { name: 'Eliminar borrador' }));
    const dialog = screen.getByRole('dialog');
    await user.dblClick(within(dialog).getByRole('button', { name: 'Eliminar borrador' }));
    expect(within(dialog).getByRole('button', { name: 'Eliminando…' })).toBeDisabled();
    expect(within(card).getByRole('button', { name: 'Continuar borrador' })).toBeDisabled();
    expect(within(card).getByRole('button', { name: 'Eliminando…' })).toBeDisabled();
    expect(within(otherCard).getByRole('button', { name: 'Continuar borrador' })).toBeEnabled();
    expect(within(otherCard).getByRole('button', { name: 'Eliminar borrador' })).toBeEnabled();
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE')).toHaveLength(1);
    await act(async () => { finish({ ok: true, status: 204 } as Response); });
    await screen.findByText('Borrador eliminado.');
    expect(window.location.pathname).toBe('/communications');
  });

  it('keeps unaffected cards visible while the success refetch is pending', async () => {
    let finish!: (response: Response) => void;
    const refreshResponse = new Promise<Response>((resolve) => { finish = resolve; });
    setup('DRAFT', undefined, { list: true, extraDraft: true, refreshResponse });
    const user = userEvent.setup();
    const card = (await screen.findByRole('heading', { name: 'Seguimiento' })).closest('article')!;
    await user.click(within(card).getByRole('button', { name: 'Eliminar borrador' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar borrador' }));
    expect(screen.queryByRole('heading', { name: 'Seguimiento' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Otro borrador' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continuar borrador' })).toBeEnabled();
    await act(async () => { finish(json([{ ...draft, id: 'communication-2', subject: 'Otro borrador' }])); });
    expect(screen.getByRole('heading', { name: 'Otro borrador' })).toBeInTheDocument();
  });
});

function json(body: unknown) { return { ok: true, json: async () => body } as Response; }
