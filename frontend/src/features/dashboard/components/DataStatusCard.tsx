import { LoaderCircle } from 'lucide-react';

import type { AcademicPeriod } from '../../alerts/api/fetchAcademicPeriods';

type DataStatusCardProps = {
  connected: boolean;
  period: AcademicPeriod | null;
  syncing: boolean;
  syncSucceeded: boolean;
  syncError: string | null;
  onSync: () => void;
};

export function DataStatusCard({ connected, period, syncing, syncSucceeded, syncError, onSync }: DataStatusCardProps) {
  const canSync = connected && !!period?.externalId && !syncing;

  return <section className="data-status-card" aria-labelledby="data-status-title">
    <div>
      <p className="eyebrow">Operación</p>
      <h2 id="data-status-title">Estado de datos</h2>
      <p className="data-status-copy">Idukay {connected ? 'conectado' : 'desconectado'} · {period ? `${period.abbreviation ? `${period.abbreviation} · ` : ''}${period.name}` : 'Sin período seleccionado'}</p>
    </div>
    <div className="data-status-action">
      <button className="btn btn-primary" type="button" disabled={!canSync} onClick={onSync}>
        {syncing && <LoaderCircle className="sync-spinner" size={16} aria-hidden="true" />}
        {syncing ? 'Sincronizando…' : 'Sincronizar ahora'}
      </button>
      {!connected && <p className="data-status-help">Conecta Idukay desde Integraciones para actualizar los datos.</p>}
      {syncSucceeded && !syncError && <p className="sync-feedback is-success" role="status">Sincronización completada en esta sesión. La última sincronización persistente aún no está disponible.</p>}
      {syncError && <div className="sync-feedback is-error" role="alert"><span>No se pudo completar la sincronización.</span><button className="text-button" type="button" disabled={!connected || !period?.externalId || syncing} onClick={onSync}>Reintentar</button></div>}
    </div>
  </section>;
}
