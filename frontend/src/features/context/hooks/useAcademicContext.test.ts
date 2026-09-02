import {
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

import { useAcademicContext } from './useAcademicContext';

describe(
  'useAcademicContext',
  () => {
    afterEach(() => {
      vi.unstubAllGlobals();
    });

    it(
      'bootstraps once on startup and exposes the context IDs',
      async () => {
        const fetchMock = vi.fn()
          .mockResolvedValue({
            ok: true,
            json: async () => ({
              institutionId:
                'context-institution',
              teacherUserId:
                'context-teacher',
            }),
          });

        vi.stubGlobal(
          'fetch',
          fetchMock,
        );

        const { result } =
          renderHook(() =>
            useAcademicContext(),
          );

        await waitFor(() => {
          expect(
            result.current.loading,
          ).toBe(false);
        });

        expect(fetchMock).toHaveBeenCalledTimes(1);

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

        expect(
          result.current.institutionId,
        ).toBe('context-institution');

        expect(
          result.current.teacherUserId,
        ).toBe('context-teacher');

        expect(
          result.current.error,
        ).toBeNull();
      },
    );

    it(
      'exposes a bootstrap error',
      async () => {
        const fetchMock = vi.fn()
          .mockResolvedValue({
            ok: false,
            status: 500,
          });

        vi.stubGlobal(
          'fetch',
          fetchMock,
        );

        const { result } =
          renderHook(() =>
            useAcademicContext(),
          );

        await waitFor(() => {
          expect(
            result.current.loading,
          ).toBe(false);
        });

        expect(
          result.current.institutionId,
        ).toBeNull();

        expect(
          result.current.teacherUserId,
        ).toBeNull();

        expect(
          result.current.error,
        ).toBe(
          'No se pudo inicializar el contexto académico (500).',
        );
      },
    );
  },
);
