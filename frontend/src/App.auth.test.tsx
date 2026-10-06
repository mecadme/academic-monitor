import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import App from './App';

const session = { user: { id: 'teacher-a', email: 'teacher@example.com', systemRole: 'USER' }, institution: { id: 'school-a', name: 'Colegio A', role: 'TEACHER' } };

function setup(options: { authenticated?: boolean; loginStatus?: number; refreshSuccess?: boolean; multiple?: boolean } = {}) {
  let authenticated = options.authenticated ?? false;
  let selected = false;
  const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    const path = new URL(url).pathname;
    if (path.endsWith('/csrf')) return Promise.resolve(new Response(null, { status: 204 }));
    if (path.endsWith('/me')) return Promise.resolve(authenticated ? Response.json(session) : new Response(null, { status: 401 }));
    if (path.endsWith('/refresh')) { authenticated = options.refreshSuccess ?? false; return Promise.resolve(new Response(null, { status: authenticated ? 204 : 401 })); }
    if (path.endsWith('/login')) {
      if (options.multiple && !selected) {
        selected = true;
        return Promise.resolve(Response.json({ code: 'INSTITUTION_SELECTION_REQUIRED', institutions: [
          { institutionId: 'school-a', institutionName: 'Colegio A', institutionRole: 'TEACHER' },
          { institutionId: 'school-b', institutionName: 'Colegio B', institutionRole: 'ADMIN' },
        ] }, { status: 409 }));
      }
      if (options.loginStatus) return Promise.resolve(Response.json({ detail: 'Detalle interno que no se debe mostrar' }, { status: options.loginStatus }));
      authenticated = true;
      return Promise.resolve(Response.json(session));
    }
    if (path.endsWith('/logout')) { authenticated = false; return Promise.resolve(new Response(null, { status: 204 })); }
    if (path.endsWith('/academic-periods')) return Promise.resolve(Response.json({ institutionId: 'school-a', teacherUserId: 'teacher-a', periods: [] }));
    if (path.endsWith('/dashboard')) return Promise.resolve(Response.json({ institutionId: 'school-a', teacherUserId: 'teacher-a', summary: {}, courses: [] }));
    if (path.endsWith('/notifications')) return Promise.resolve(Response.json({ items: [], unreadCount: 0 }));
    throw new Error(`Unexpected test route: ${path} ${init?.method ?? 'GET'}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); localStorage.clear(); sessionStorage.clear(); window.history.replaceState({}, '', '/'); });

async function enterCredentials() {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('Correo electrónico'), 'teacher@example.com');
  await user.type(screen.getByLabelText('Contraseña'), 'example-test-password');
  return user;
}

describe('authentication', () => {
  it('shows accessible login at /login and never loads protected data before authentication', async () => {
    const fetchMock = setup();
    await screen.findByRole('heading', { name: 'Iniciar sesión' });
    await waitFor(() => {
      expect(window.location.pathname).toBe('/login');
    });
    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('autocomplete', 'email');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('autocomplete', 'current-password');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password');
    expect(fetchMock.mock.calls.every(([url]) => new URL(url).pathname.startsWith('/api/v1/auth/'))).toBe(true);
  });

  it('logs in by keyboard, shows the current email, and does not store credentials or tokens', async () => {
    const storage = vi.spyOn(Storage.prototype, 'setItem');
    const fetchMock = setup();
    const user = await enterCredentials();
    await user.keyboard('{Enter}');
    expect(await screen.findByText(session.user.email)).toBeInTheDocument();
    await waitFor(() => expect(window.location.pathname).toBe('/'));
    expect(storage).not.toHaveBeenCalled();
    const request = fetchMock.mock.calls.find(([url]) => url.endsWith('/login'))?.[1];
    expect(request?.credentials).toBe('include');
    expect(JSON.parse(request?.body as string)).toEqual({ email: session.user.email, password: 'example-test-password' });
    expect(new Headers(request?.headers).get('X-XSRF-TOKEN')).toBe('test-csrf');
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument();
  });

  it('shows a generic credential failure and clears the password field', async () => {
    setup({ loginStatus: 401 });
    const user = await enterCredentials();
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.');
    expect(screen.queryByText(/Detalle interno/)).not.toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toHaveValue('');
  });

  it('restores a valid session with /me', async () => {
    const fetchMock = setup({ authenticated: true });
    expect(await screen.findByText(session.user.email)).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/me'))).toBe(true);
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/refresh'))).toBe(false);
  });

  it('allows a new teacher without academic periods to open integrations', async () => {
    window.history.replaceState({}, '', '/settings/integrations');
    setup({ authenticated: true });
    expect(await screen.findByRole('heading', { name: 'Integraciones' })).toBeInTheDocument();
    expect(screen.queryByText('No hay períodos disponibles')).not.toBeInTheDocument();
  });

  it('refreshes expired access before restoring the application', async () => {
    const fetchMock = setup({ refreshSuccess: true });
    expect(await screen.findByText(session.user.email)).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1);
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/me'))).toHaveLength(2);
  });

  it('shows login after a rejected refresh', async () => {
    setup({ refreshSuccess: false });
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Navegación principal')).not.toBeInTheDocument();
  });

  it('logs out through the user menu and unmounts protected state', async () => {
    const fetchMock = setup({ authenticated: true });
    const user = userEvent.setup();
    await user.click(await screen.findByText(session.user.email));
    expect(screen.getByText('Docente')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    await screen.findByRole('heading', { name: 'Iniciar sesión' });
    expect(window.location.pathname).toBe('/login');
    expect(fetchMock.mock.calls.find(([url]) => url.endsWith('/logout'))?.[1]).toMatchObject({ method: 'POST', credentials: 'include' });
    expect(screen.queryByText(session.user.email)).not.toBeInTheDocument();
  });

  it('requires explicit institution selection and repeats login with that choice', async () => {
    const fetchMock = setup({ multiple: true });
    const user = await enterCredentials();
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    const select = await screen.findByLabelText('Institución');
    expect(select).toHaveValue('');
    await user.selectOptions(select, 'school-a');
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByText(session.user.email)).toBeInTheDocument();
    const calls = fetchMock.mock.calls.filter(([url]) => url.endsWith('/login'));
    expect(calls).toHaveLength(2);
    expect(JSON.parse(calls[1][1]?.body as string)).toMatchObject({ institutionId: 'school-a', password: 'example-test-password' });
  });

  it('never sends identity IDs as ownership query parameters after restoring a session', async () => {
    const fetchMock = setup({ authenticated: true });
    await screen.findByText(session.user.email);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url.includes('/notifications'))).toBe(true));
    for (const [url, init] of fetchMock.mock.calls) {
      expect(url).not.toMatch(/institutionId=|teacherUserId=|context\/bootstrap/);
      expect(init?.body).toBeUndefined();
      expect(init?.credentials).toBe('include');
    }
  });
});
