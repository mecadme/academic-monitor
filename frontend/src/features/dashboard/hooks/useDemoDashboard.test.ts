import {
  act,
  renderHook,
  waitFor,
} from '@testing-library/react';

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import { useDemoDashboard } from './useDemoDashboard';

const {
  mockGetDashboard,
  mockSyncDemo,
} = vi.hoisted(() => ({
  mockGetDashboard: vi.fn(),
  mockSyncDemo: vi.fn(),
}));

vi.mock(
  '../../../api/demo',
  () => ({
    getDashboard: mockGetDashboard,
    syncDemo: mockSyncDemo,
  }),
);

const syncResponse = {
  institutionId: 'institution-1',
  teacherUserId: 'teacher-1',
};

const initialDashboard = {
  courseId: 'course-1',
  courseName: 'Course 1',
};

const refreshedDashboard = {
  courseId: 'course-1',
  courseName: 'Updated Course 1',
};

describe(
  'useDemoDashboard',
  () => {
    beforeEach(() => {
      vi.clearAllMocks();

      mockSyncDemo.mockResolvedValue(
        syncResponse,
      );

      mockGetDashboard.mockResolvedValue(
        initialDashboard,
      );
    });

    it(
      'loads the initial scenario and exposes its academic context',
      async () => {
        const { result } =
          renderHook(() =>
            useDemoDashboard(),
          );

        await waitFor(() => {
          expect(
            result.current.loading,
          ).toBe(false);
        });

        expect(
          mockSyncDemo,
        ).toHaveBeenCalledWith(
          'INITIAL',
        );

        expect(
          mockGetDashboard,
        ).toHaveBeenCalledWith(
          'teacher-1',
        );

        expect(
          result.current.institutionId,
        ).toBe('institution-1');

        expect(
          result.current.teacherUserId,
        ).toBe('teacher-1');

        expect(
          result.current.dashboard,
        ).toEqual(initialDashboard);

        expect(
          result.current.error,
        ).toBeNull();
      },
    );

    it(
      'refreshes the dashboard for the synchronized teacher',
      async () => {
        mockGetDashboard
          .mockResolvedValueOnce(
            initialDashboard,
          )
          .mockResolvedValueOnce(
            refreshedDashboard,
          );

        const { result } =
          renderHook(() =>
            useDemoDashboard(),
          );

        await waitFor(() => {
          expect(
            result.current.loading,
          ).toBe(false);
        });

        await act(async () => {
          await result.current
            .refreshDashboard();
        });

        expect(
          mockGetDashboard,
        ).toHaveBeenNthCalledWith(
          2,
          'teacher-1',
        );

        expect(
          result.current.dashboard,
        ).toEqual(refreshedDashboard);
      },
    );
  },
);
