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

import type { AcademicPeriodCatalog } from '../api/fetchAcademicPeriods';
import { useAcademicPeriods } from './useAcademicPeriods';

const catalog: AcademicPeriodCatalog = {
  institutionId: 'institution-1',
  teacherUserId: 'teacher-1',
  periods: [
    {
      id: 'period-t1-internal',
      name: 'Primer trimestre',
      abbreviation: 'T1',
      order: 1,
      synchronized: true,
    },
  ],
};

describe('useAcademicPeriods', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('waits for both context IDs and then loads the neutral period catalog', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(catalog));
    vi.stubGlobal('fetch', fetchMock);

    const { result, rerender } = renderHook(
      ({
        institutionId,
        teacherUserId,
      }: {
        institutionId: string | null;
        teacherUserId: string | null;
      }) =>
        useAcademicPeriods({
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

    expect(fetchMock).not.toHaveBeenCalled();

    rerender({
      institutionId: 'institution-1',
      teacherUserId: 'teacher-1',
    });

    await waitFor(() => {
      expect(result.current.catalog).toEqual(catalog);
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      '/api/v1/academic-periods?institutionId=institution-1&teacherUserId=teacher-1',
    );
  });

  it('preserves the last catalog during refresh and owns retry errors', async () => {
    const refreshRequest = deferred<Response>();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(catalog))
      .mockReturnValueOnce(refreshRequest.promise)
      .mockResolvedValueOnce(jsonResponse(catalog));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() =>
      useAcademicPeriods({
        institutionId: 'institution-1',
        teacherUserId: 'teacher-1',
      }),
    );

    await waitFor(() => {
      expect(result.current.catalog).toEqual(catalog);
    });

    act(() => {
      void result.current.refresh();
    });
    await waitFor(() => {
      expect(result.current.loading).toBe(true);
    });
    expect(result.current.catalog).toEqual(catalog);

    await act(async () => {
      refreshRequest.resolve({
        ok: false,
        status: 503,
      } as Response);
      await refreshRequest.promise;
    });
    await waitFor(() => {
      expect(result.current.error).toContain('(503)');
    });

    await act(async () => {
      await result.current.refresh();
    });
    expect(result.current.error).toBeNull();
    expect(result.current.catalog).toEqual(catalog);
  });
});

function jsonResponse(value: AcademicPeriodCatalog) {
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
