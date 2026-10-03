import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { DataStatusCard } from './DataStatusCard';

const period = { id: 'period-1', academicYearId: 'year-1', externalId: 'external-period-1', academicYear: '2026–2027', name: 'Trimestre 1', abbreviation: 'T1', order: 1, synchronized: true };

describe('DataStatusCard', () => {
  it('uses the selected global period and prevents a duplicate sync while loading', () => {
    render(<DataStatusCard connected period={period} syncing syncSucceeded={false} syncError={null} onSync={vi.fn()} />);
    expect(screen.getByText(/T1 · Trimestre 1/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sincronizando…' })).toBeDisabled();
  });

  it('reports session success and provides a retry after an error', async () => {
    const onSync = vi.fn(); const user = userEvent.setup();
    const { rerender } = render(<DataStatusCard connected period={period} syncing={false} syncSucceeded syncError={null} onSync={onSync} />);
    expect(screen.getByText(/Sincronización completada en esta sesión/)).toBeInTheDocument();
    rerender(<DataStatusCard connected period={period} syncing={false} syncSucceeded={false} syncError="network" onSync={onSync} />);
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo completar la sincronización.');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(onSync).toHaveBeenCalledTimes(1);
  });
});
