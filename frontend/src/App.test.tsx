import {
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';
import App from './App';

vi.mock(
  './features/idukay/lib/idukayFingerprint',
  () => ({
    createIdukayFingerprint: vi.fn()
      .mockResolvedValue({
        user_agent: 'test',
      }),
  }),
);

const contextResponse = {
  institutionId: 'context-institution',
  teacherUserId: 'context-teacher',
};

const dashboardResponse = {
  institutionId: 'context-institution',
  teacherUserId: 'context-teacher',
  summary: {
    courses: 2,
    students: 45,
    activities: 37,
    openAlerts: 21,
    warnings: 13,
    critical: 8,
  },
  courses: [
    {
      id: 'course-1',
      name: '1.º BGU A',
      subject: 'Física',
      academicYear: '2025 - 2026',
      students: 32,
      activities: 24,
      openAlerts: 18,
      warnings: 11,
      critical: 7,
    },
    {
      id: 'course-2',
      name: '1.º BGU B',
      subject: 'Química',
      academicYear: '2025 - 2026',
      students: 20,
      activities: 13,
      openAlerts: 3,
      warnings: 2,
      critical: 1,
    },
  ],
};

const idukaySyncResponse = {
  coursesProcessed: 13,
  gradesProcessed: 2500,
  openAlerts: 230,
  warnings: 160,
  critical: 70,
};

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders persisted academic dashboard data without calling DEMO', async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(
      screen.getByText(
        /inicializando contexto académico/i,
      ),
    ).toBeInTheDocument();

    expect(
      await screen.findByText('1.º BGU A'),
    ).toBeInTheDocument();
    expect(screen.getByText('Física')).toBeInTheDocument();
    expect(screen.getByText('Química')).toBeInTheDocument();
    expect(
      screen.getAllByText('2025 - 2026'),
    ).toHaveLength(2);
    expect(
      screen.getByRole('heading', {
        name: 'Resumen por curso',
      }),
    ).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/context/bootstrap',
      ),
      {
        method: 'POST',
        headers: {
          Accept: 'application/json',
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v1/dashboard?institutionId=context-institution&teacherUserId=context-teacher',
      ),
      expect.objectContaining({
        method: 'GET',
      }),
    );
    expect(
      fetchMock.mock.calls.some(([url]) =>
        String(url).includes('/api/v1/demo/'),
      ),
    ).toBe(false);
  });

  it('uses the neutral context IDs for the Idukay integration path', async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await screen.findByText('1.º BGU A');
    await connectIdukay(user);

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([url]) =>
          String(url).includes(
            '/api/v1/integrations/idukay/test-periods',
          ),
        ),
      ).toBe(true);
    });

    const loginCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes(
        '/api/v1/integrations/idukay/test-login',
      ),
    );
    expect(loginCall).toBeDefined();

    const loginRequest = loginCall?.[1] as RequestInit;
    const loginBody = JSON.parse(
      String(loginRequest.body),
    );
    expect(loginBody).toMatchObject({
      institutionId: 'context-institution',
      teacherUserId: 'context-teacher',
    });

    const periodsCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes(
        '/api/v1/integrations/idukay/test-periods',
      ),
    );
    expect(String(periodsCall?.[0])).toContain(
      'institutionId=context-institution',
    );
    expect(String(periodsCall?.[0])).toContain(
      'teacherUserId=context-teacher',
    );
  });

  it('refreshes the academic dashboard after a successful Idukay sync', async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await screen.findByText('1.º BGU A');
    await connectIdukay(user);

    await user.click(
      await screen.findByRole('button', {
        name: 'Sincronizar T1',
      }),
    );

    await waitFor(() => {
      const dashboardCalls = fetchMock.mock.calls.filter(
        ([url]) =>
          String(url).includes('/api/v1/dashboard?'),
      );
      expect(dashboardCalls).toHaveLength(2);
    });

    const syncCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes(
        '/api/v1/integrations/idukay/test-sync',
      ),
    );
    expect(String(syncCall?.[0])).toContain(
      'institutionId=context-institution',
    );
    expect(String(syncCall?.[0])).toContain(
      'teacherUserId=context-teacher',
    );
  });

  it('shows a neutral context bootstrap failure', async () => {
    const fetchMock = createFetchMock({
      contextFails: true,
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(
      await screen.findByText(
        'No se pudo inicializar el contexto académico (500).',
      ),
    ).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});

async function connectIdukay(
  user: ReturnType<typeof userEvent.setup>,
) {
  await user.type(
    screen.getByRole('textbox', {
      name: /correo de idukay/i,
    }),
    'teacher@example.com',
  );
  await user.type(
    screen.getByLabelText(/contraseña/i),
    'secret-password',
  );
  await user.click(
    screen.getByRole('button', {
      name: /conectar idukay/i,
    }),
  );
}

function createFetchMock(
  options: {
    contextFails?: boolean;
  } = {},
) {
  return vi.fn(
    async (...request: Parameters<typeof fetch>) => {
      const [input] = request;
      const url = String(input);

      if (url.includes('/api/v1/context/bootstrap')) {
        if (options.contextFails) {
          return {
            ok: false,
            status: 500,
          } as Response;
        }

        return {
          ok: true,
          json: async () => contextResponse,
        } as Response;
      }

      if (url.includes('/api/v1/dashboard')) {
        return {
          ok: true,
          json: async () => dashboardResponse,
        } as Response;
      }

      if (
        url.includes(
          '/api/v1/integrations/idukay/test-login',
        )
      ) {
        return {
          ok: true,
          json: async () => ({
            authenticated: true,
          }),
        } as Response;
      }

      if (
        url.includes(
          '/api/v1/integrations/idukay/test-periods',
        )
      ) {
        return {
          ok: true,
          json: async () => ({
            academicYearId: 'year-1',
            academicYear: '2025 - 2026',
            baseScore: 10,
            periods: [
              {
                id: 'period-t1',
                name: 'Trimestre 1',
                abbreviation: 'T1',
              },
            ],
          }),
        } as Response;
      }

      if (
        url.includes(
          '/api/v1/integrations/idukay/test-sync',
        )
      ) {
        return {
          ok: true,
          json: async () => idukaySyncResponse,
        } as Response;
      }

      throw new Error(`Unexpected request: ${url}`);
    },
  );
}
