import {
  act,
  renderHook,
  waitFor,
} from '@testing-library/react';
import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import { useAcademicDashboard } from './useAcademicDashboard';

const dashboardResponse = {
  institutionId: 'institution-1',
  teacherUserId: 'teacher-1',
  summary: {
    courses: 1,
    students: 32,
    activities: 24,
    openAlerts: 18,
    warnings: 11,
    critical: 7,
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
  ],
};

describe('useAcademicDashboard', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('does not fetch until both context IDs are available', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const { rerender } = renderHook(
      ({
        institutionId,
        teacherUserId,
      }: {
        institutionId: string | null;
        teacherUserId: string | null;
      }) =>
        useAcademicDashboard({
          institutionId,
          teacherUserId,
        }),
      {
        initialProps: {
          institutionId: null as string | null,
          teacherUserId: null as string | null,
        },
      },
    );

    rerender({
      institutionId: 'institution-1',
      teacherUserId: null,
    });
    rerender({
      institutionId: null,
      teacherUserId: 'teacher-1',
    });

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('calls the production endpoint with neutral context IDs and exposes data', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => dashboardResponse,
    });
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() =>
      useAcademicDashboard({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.dashboard).toEqual(
        dashboardResponse,
      );
    });

    const [url, request] = fetchMock.mock.calls[0];
    expect(String(url)).toContain(
      '/api/v1/dashboard?institutionId=institution-1&teacherUserId=teacher-1',
    );
    expect(request).toMatchObject({
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('exposes API errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 503,
      }),
    );

    const { result } = renderHook(() =>
      useAcademicDashboard({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.error).toBe(
        'No se pudo cargar el dashboard académico (503).',
      );
    });

    expect(result.current.dashboard).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it('refreshes the current context on demand', async () => {
    const refreshed = {
      ...dashboardResponse,
      summary: {
        ...dashboardResponse.summary,
        activities: 25,
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => dashboardResponse,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => refreshed,
      });
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() =>
      useAcademicDashboard({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.dashboard).toEqual(
        dashboardResponse,
      );
    });

    await act(async () => {
      await result.current.refresh();
    });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(result.current.dashboard).toEqual(refreshed);
  });
});
