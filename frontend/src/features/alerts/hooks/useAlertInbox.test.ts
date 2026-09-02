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

import type { AlertInbox } from '../api/fetchAlertInbox';
import { useAlertInbox } from './useAlertInbox';

const allCoursesInbox = inbox(
  'alert-all',
  'Course A',
);
const filteredInbox = inbox(
  'alert-filtered',
  'Course B',
);

describe('useAlertInbox', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('does not fetch before both academic context IDs are available', () => {
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
        useAlertInbox({
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

  it('fetches the production inbox with neutral context IDs', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(allCoursesInbox),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() =>
      useAlertInbox({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.inbox).toEqual(
        allCoursesInbox,
      );
    });

    const [url, request] = fetchMock.mock.calls[0];
    expect(String(url)).toContain(
      '/api/v1/alerts?institutionId=institution-1&teacherUserId=teacher-1',
    );
    expect(String(url)).not.toContain('courseId=');
    expect(request).toMatchObject({
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('adds and removes courseId while preserving the previous inbox during refresh', async () => {
    const filteredRequest = deferred<Response>();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(allCoursesInbox),
      )
      .mockReturnValueOnce(filteredRequest.promise)
      .mockResolvedValueOnce(
        jsonResponse(allCoursesInbox),
      );
    vi.stubGlobal('fetch', fetchMock);

    const { result, rerender } = renderHook(
      ({ courseId }: { courseId: string | null }) =>
        useAlertInbox({
          institutionId: 'institution-1',
          teacherUserId: 'teacher-1',
          courseId,
        }),
      {
        initialProps: {
          courseId: null as string | null,
        },
      },
    );

    await waitFor(() => {
      expect(result.current.inbox).toEqual(
        allCoursesInbox,
      );
    });

    rerender({ courseId: 'course-2' });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2);
      expect(result.current.loading).toBe(true);
    });
    expect(result.current.inbox).toEqual(allCoursesInbox);
    expect(String(fetchMock.mock.calls[1][0])).toContain(
      'courseId=course-2',
    );

    await act(async () => {
      filteredRequest.resolve(
        jsonResponse(filteredInbox),
      );
      await filteredRequest.promise;
    });

    await waitFor(() => {
      expect(result.current.inbox).toEqual(
        filteredInbox,
      );
    });

    rerender({ courseId: null });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(3);
    });
    expect(String(fetchMock.mock.calls[2][0])).not.toContain(
      'courseId=',
    );
  });

  it('composes course and period filters and removes the period for all periods', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(allCoursesInbox),
    );
    vi.stubGlobal('fetch', fetchMock);

    const { rerender } = renderHook(
      ({
        courseId,
        academicPeriodId,
      }: {
        courseId: string | null;
        academicPeriodId: string | null;
      }) =>
        useAlertInbox({
          institutionId: 'institution-1',
          teacherUserId: 'teacher-1',
          courseId,
          academicPeriodId,
        }),
      {
        initialProps: {
          courseId: 'course-2' as string | null,
          academicPeriodId: 'period-t1-internal' as string | null,
        },
      },
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      'courseId=course-2&academicPeriodId=period-t1-internal',
    );

    rerender({
      courseId: 'course-2',
      academicPeriodId: null,
    });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2);
    });
    expect(String(fetchMock.mock.calls[1][0])).toContain(
      'courseId=course-2',
    );
    expect(String(fetchMock.mock.calls[1][0])).not.toContain(
      'academicPeriodId=',
    );
  });

  it('prevents an older request from replacing a newer course result', async () => {
    const firstRequest = deferred<Response>();
    const secondRequest = deferred<Response>();
    const fetchMock = vi
      .fn()
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise);
    vi.stubGlobal('fetch', fetchMock);

    const { result, rerender } = renderHook(
      ({ courseId }: { courseId: string | null }) =>
        useAlertInbox({
          institutionId: 'institution-1',
          teacherUserId: 'teacher-1',
          courseId,
        }),
      {
        initialProps: {
          courseId: null as string | null,
        },
      },
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    rerender({ courseId: 'course-2' });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    await act(async () => {
      secondRequest.resolve(
        jsonResponse(filteredInbox),
      );
      await secondRequest.promise;
    });

    await waitFor(() => {
      expect(result.current.inbox).toEqual(
        filteredInbox,
      );
    });

    await act(async () => {
      firstRequest.resolve(
        jsonResponse(allCoursesInbox),
      );
      await firstRequest.promise;
    });

    expect(result.current.inbox).toEqual(filteredInbox);
    expect(
      (fetchMock.mock.calls[0][1] as RequestInit)
        .signal?.aborted,
    ).toBe(true);
  });

  it('owns its error state and retries locally', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
      } as Response)
      .mockResolvedValueOnce(
        jsonResponse(allCoursesInbox),
      );
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() =>
      useAlertInbox({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.error).toBe(
        'No se pudieron cargar las alertas (503).',
      );
    });

    await act(async () => {
      await result.current.refresh();
    });

    expect(result.current.inbox).toEqual(allCoursesInbox);
    expect(result.current.error).toBeNull();
  });
});

function inbox(
  alertId: string,
  courseName: string,
): AlertInbox {
  return {
    institutionId: 'institution-1',
    teacherUserId: 'teacher-1',
    total: 1,
    alerts: [
      {
        id: alertId,
        severity: 'CRITICAL',
        ruleCode: 'LOW_GRADE',
        score: 4.5,
        course: {
          id: 'course-1',
          name: courseName,
          subject: 'Física',
        },
        activity: {
          id: 'activity-1',
          name: 'Movimiento rectilíneo',
          maximumScore: 10,
          dueDate: '2025-11-07',
        },
        student: {
          id: 'student-1',
          name: 'Ana Torres',
        },
      },
    ],
  };
}

function jsonResponse(value: AlertInbox) {
  return {
    ok: true,
    json: async () => value,
  } as Response;
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise;
  });

  return { promise, resolve };
}
