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

const syncResponse = {
  institutionId: 'institution-1',
  teacherUserId: 'teacher-1',
  courseId: 'course-1',
  courseName: '1.º BGU A',
  scenario: 'INITIAL',
  students: 4,
  gradesProcessed: 4,
  openAlerts: 3,
  warnings: 2,
  critical: 1,
};

const dashboardResponse = {
  courseId: 'course-1',
  courseName: '1.º BGU A',
  subject: 'Física',
  activity: {
    id: 'activity-1',
    name: 'Movimiento rectilíneo',
  },
  summary: {
    totalStudents: 4,
    openAlerts: 3,
    warnings: 2,
    critical: 1,
    resolvedAlerts: 0,
  },
  students: [
    {
      id: 'student-1',
      name: 'Ana Torres',
      score: 9.2,
      status: 'OK',
    },
    {
      id: 'student-2',
      name: 'Carlos Vega',
      score: 7,
      status: 'WARNING',
    },
    {
      id: 'student-3',
      name: 'Mateo Cárdenas',
      score: 4.8,
      status: 'CRITICAL',
    },
  ],
};

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('loads and renders the initial academic monitoring dashboard', async () => {
    const fetchMock = createFetchMock();

    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(screen.getByText(/preparando entorno de demostración/i)).toBeInTheDocument();

    expect(
      await screen.findByRole('heading', {
        name: '1.º BGU A',
      }),
    ).toBeInTheDocument();

    expect(screen.getByText('Física')).toBeInTheDocument();

    expect(
      screen.getByRole('heading', {
        name: 'Movimiento rectilíneo',
      }),
    ).toBeInTheDocument();

    expect(screen.getByText('Ana Torres')).toBeInTheDocument();

    expect(screen.getByText('Mateo Cárdenas')).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledTimes(3);

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/context/bootstrap'),
      {
        method: 'POST',
        headers: {
          Accept: 'application/json',
        },
      },
    );

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/demo/sync?scenario=INITIAL'),
      {
        method: 'POST',
      },
    );

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/demo/dashboard?teacherUserId=teacher-1'),
    );
  });

  it('uses the neutral context IDs for the Idukay integration path', async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();

    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await screen.findByRole('heading', {
      name: '1.º BGU A',
    });

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

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          ([url]) => String(url).includes(
            '/api/v1/integrations/idukay/test-periods',
          ),
        ),
      ).toBe(true);
    });

    const loginCall = fetchMock.mock.calls.find(
      ([url]) => String(url).includes(
        '/api/v1/integrations/idukay/test-login',
      ),
    );

    expect(loginCall).toBeDefined();

    const loginRequest = loginCall?.[1] as RequestInit;
    const loginBody = JSON.parse(String(loginRequest.body));

    expect(loginBody).toMatchObject({
      institutionId: 'context-institution',
      teacherUserId: 'context-teacher',
    });

    expect(loginBody).not.toMatchObject({
      institutionId: 'institution-1',
      teacherUserId: 'teacher-1',
    });

    const periodsCall = fetchMock.mock.calls.find(
      ([url]) => String(url).includes(
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
  });
});

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
          };
        }

        return {
          ok: true,
          json: async () => contextResponse,
        };
      }

      if (url.includes('/api/v1/demo/sync')) {
        return {
          ok: true,
          json: async () => syncResponse,
        };
      }

      if (url.includes('/api/v1/demo/dashboard')) {
        return {
          ok: true,
          json: async () => dashboardResponse,
        };
      }

      if (url.includes('/api/v1/integrations/idukay/test-login')) {
        return {
          ok: true,
          json: async () => ({
            authenticated: true,
          }),
        };
      }

      if (url.includes('/api/v1/integrations/idukay/test-periods')) {
        return {
          ok: true,
          json: async () => ({
            academicYearId: 'year-1',
            academicYear: '2025 - 2026',
            baseScore: 10,
            periods: [],
          }),
        };
      }

      throw new Error(`Unexpected request: ${url}`);
    },
  );
}
