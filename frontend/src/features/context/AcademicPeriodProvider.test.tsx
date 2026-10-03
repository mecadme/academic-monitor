import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AcademicPeriodProvider, useAcademicPeriod } from './AcademicPeriodProvider';

const context = { institutionId: 'institution-1', teacherUserId: 'teacher-1' };
const catalog = { ...context, periods: [
  { id: 'period-old', academicYearId: 'year-old', externalId: 'external-old', academicYear: '2025–2026', name: 'Trimestre 3', abbreviation: 'T3', order: 3, synchronized: true },
  { id: 'period-new', academicYearId: 'year-new', externalId: 'external-new', academicYear: '2026–2027', name: 'Trimestre 1', abbreviation: 'T1', order: 1, synchronized: true },
] };

describe('AcademicPeriodProvider', () => {
  afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); });

  it('filters periods by the selected academic year and replaces an invalid prior period', async () => {
    window.localStorage.setItem('academic-monitor:academic-context:institution-1:teacher-1:year', 'year-new');
    window.localStorage.setItem('academic-monitor:academic-context:institution-1:teacher-1:period:year-new', 'period-old');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(catalog)));
    const user = userEvent.setup();
    render(<AcademicPeriodProvider {...context}><Probe /></AcademicPeriodProvider>);
    await waitFor(() => expect(screen.getByTestId('period')).toHaveTextContent('period-new'));
    expect(screen.getByTestId('years')).toHaveTextContent('year-new,year-old');
    await user.click(screen.getByRole('button', { name: 'Año anterior' }));
    await waitFor(() => expect(screen.getByTestId('period')).toHaveTextContent('period-old'));
    expect(screen.getByTestId('periods')).toHaveTextContent('period-old');
    expect(screen.getByTestId('periods')).not.toHaveTextContent('period-new');
  });
});

function Probe() { const academic = useAcademicPeriod(); return <><output data-testid="years">{academic.academicYears.map((year) => year.id).join(',')}</output><output data-testid="periods">{academic.periods.map((period) => period.id).join(',')}</output><output data-testid="period">{academic.selectedPeriodId}</output><button type="button" onClick={() => academic.selectAcademicYear('year-old')}>Año anterior</button></>; }
function json(body: unknown) { return { ok: true, json: async () => body } as Response; }
