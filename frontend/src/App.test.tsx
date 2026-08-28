import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

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
    const fetchMock = vi.fn();

    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        json: async () => syncResponse,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => dashboardResponse,
      });

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

    expect(fetchMock).toHaveBeenCalledTimes(2);

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('/api/v1/demo/sync?scenario=INITIAL'),
      {
        method: 'POST',
      },
    );

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining('/api/v1/demo/dashboard?teacherUserId=teacher-1'),
    );
  });
});
