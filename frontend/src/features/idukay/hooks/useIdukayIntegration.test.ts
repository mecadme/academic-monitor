import {
  act,
  renderHook,
} from '@testing-library/react';

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import { useIdukayIntegration } from './useIdukayIntegration';

const {
  mockTestIdukayLogin,
  mockGetIdukayPeriods,
  mockSyncIdukayPeriod,
} = vi.hoisted(() => ({
  mockTestIdukayLogin: vi.fn(),
  mockGetIdukayPeriods: vi.fn(),
  mockSyncIdukayPeriod: vi.fn(),
}));

vi.mock(
  '../api/testIdukayLogin',
  () => ({
    testIdukayLogin:
    mockTestIdukayLogin,
  }),
);

vi.mock(
  '../api/getIdukayPeriods',
  () => ({
    getIdukayPeriods:
    mockGetIdukayPeriods,
  }),
);

vi.mock(
  '../api/syncIdukayPeriod',
  () => ({
    syncIdukayPeriod:
    mockSyncIdukayPeriod,
  }),
);

const context = {
  institutionId: 'institution-1',
  teacherUserId: 'teacher-1',
};

const periodsResponse = {
  academicYearId: 'year-1',
  academicYear: '2025 - 2026',
  baseScore: 10,
  periods: [
    {
      id: 'period-t1',
      name: 'Trimestre 1',
      abbreviation: 'T1',
    },
    {
      id: 'period-t2',
      name: 'Trimestre 2',
      abbreviation: 'T2',
    },
    {
      id: 'period-t3',
      name: 'Trimestre 3',
      abbreviation: 'T3',
    },
  ],
};

const syncResponse = {
  coursesProcessed: 13,
  gradesProcessed: 2500,
  openAlerts: 230,
  warnings: 160,
  critical: 70,
};

describe(
  'useIdukayIntegration',
  () => {
    beforeEach(() => {
      vi.clearAllMocks();

      mockTestIdukayLogin
        .mockResolvedValue({
          authenticated: true,
        });

      mockGetIdukayPeriods
        .mockResolvedValue(
          periodsResponse,
        );

      mockSyncIdukayPeriod
        .mockResolvedValue(
          syncResponse,
        );
    });

    it(
      'connects to Idukay and loads the available academic periods',
      async () => {
        const { result } =
          renderHook(() =>
            useIdukayIntegration(
              context,
            ),
          );

        act(() => {
          result.current.setEmail(
            'teacher@example.com',
          );

          result.current.setPassword(
            'secret-password',
          );
        });

        await act(async () => {
          await result.current.connect();
        });

        expect(
          mockTestIdukayLogin,
        ).toHaveBeenCalledWith({
          email:
            'teacher@example.com',
          password:
            'secret-password',
          institutionId:
            'institution-1',
          teacherUserId:
            'teacher-1',
        });

        expect(
          mockGetIdukayPeriods,
        ).toHaveBeenCalledWith({
          institutionId:
            'institution-1',
          teacherUserId:
            'teacher-1',
        });

        expect(
          result.current.connected,
        ).toBe(true);

        expect(
          result.current.academicYear,
        ).toBe('2025 - 2026');

        expect(
          result.current.baseScore,
        ).toBe(10);

        expect(
          result.current.periods,
        ).toEqual(
          periodsResponse.periods,
        );

        expect(
          result.current.selectedPeriodId,
        ).toBe('period-t1');

        expect(
          result.current.password,
        ).toBe('');

        expect(
          result.current.error,
        ).toBeNull();
      },
    );

    it(
      'synchronizes the selected academic period',
      async () => {
        const { result } =
          renderHook(() =>
            useIdukayIntegration(
              context,
            ),
          );

        act(() => {
          result.current.setEmail(
            'teacher@example.com',
          );

          result.current.setPassword(
            'secret-password',
          );
        });

        await act(async () => {
          await result.current.connect();
        });

        act(() => {
          result.current.selectPeriod(
            'period-t2',
          );
        });

        expect(
          result.current.selectedPeriodId,
        ).toBe('period-t2');

        await act(async () => {
          await result.current
            .synchronizeSelectedPeriod();
        });

        expect(
          mockSyncIdukayPeriod,
        ).toHaveBeenCalledWith({
          institutionId:
            'institution-1',
          teacherUserId:
            'teacher-1',
          periodExternalId:
            'period-t2',
        });

        expect(
          result.current.syncResult,
        ).toEqual(syncResponse);

        expect(
          result.current.error,
        ).toBeNull();
      },
    );

    it(
      'clears the previous synchronization result when the period changes',
      async () => {
        const { result } =
          renderHook(() =>
            useIdukayIntegration(
              context,
            ),
          );

        act(() => {
          result.current.setEmail(
            'teacher@example.com',
          );

          result.current.setPassword(
            'secret-password',
          );
        });

        await act(async () => {
          await result.current.connect();
        });

        await act(async () => {
          await result.current
            .synchronizeSelectedPeriod();
        });

        expect(
          result.current.syncResult,
        ).toEqual(syncResponse);

        act(() => {
          result.current.selectPeriod(
            'period-t3',
          );
        });

        expect(
          result.current.selectedPeriodId,
        ).toBe('period-t3');

        expect(
          result.current.syncResult,
        ).toBeNull();
      },
    );
  },
);
