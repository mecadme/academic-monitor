import type { IdukayPeriod } from '../../idukay/api/getIdukayPeriods';
import type { SyncIdukayPeriodResponse } from '../../idukay/api/syncIdukayPeriod';

type IdukayIntegrationCardProps = {
  connected: boolean;
  connecting: boolean;
  syncing: boolean;

  email: string;
  password: string;

  academicYear: string | null;
  baseScore: number | null;

  periods: IdukayPeriod[];
  selectedPeriodId: string;

  syncResult: SyncIdukayPeriodResponse | null;

  onEmailChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onConnect: () => void;
  onPeriodChange: (periodId: string) => void;
  onSync: () => void;
};

export function IdukayIntegrationCard({
                                        connected,
                                        connecting,
                                        syncing,
                                        email,
                                        password,
                                        academicYear,
                                        baseScore,
                                        periods,
                                        selectedPeriodId,
                                        syncResult,
                                        onEmailChange,
                                        onPasswordChange,
                                        onConnect,
                                        onPeriodChange,
                                        onSync,
                                      }: IdukayIntegrationCardProps) {
  const selectedPeriod =
    periods.find(
      (period) =>
        period.id === selectedPeriodId,
    ) ?? null;

  return (
    <section className="container integration-card">
    <div className="integration-heading">
    <div>
      <p className="section-label">
      Integración académica
  </p>

  <h3 className="integration-title">
    Idukay
    </h3>

    <p className="integration-description">
    Conecta tu cuenta para descubrir
  automáticamente el año lectivo y
  los períodos disponibles.
  </p>
  </div>

  <ConnectionStatus
  connected={connected}
  />
  </div>

  {!connected ? (
    <div className="idukay-login-grid">
    <label className="form-field">
      <span>
        Correo de Idukay
  </span>

  <input
    type="email"
    value={email}
    placeholder="docente@correo.com"
    autoComplete="username"
    onChange={(event) =>
    onEmailChange(
      event.target.value,
    )
  }
    />
    </label>

    <label className="form-field">
    <span>
      Contraseña
    </span>

    <input
    type="password"
    value={password}
    placeholder="••••••••"
    autoComplete="current-password"
    onChange={(event) =>
    onPasswordChange(
      event.target.value,
    )
  }
    />
    </label>

    <div className="connection-action">
  <button
    className="btn btn-primary btn-wide"
    disabled={
        connecting ||
      !email.trim() ||
      !password
  }
    onClick={onConnect}
    >
    {connecting
      ? 'Conectando...'
      : 'Conectar Idukay'}
    </button>
    </div>
    </div>
  ) : (
    <>
      <div className="idukay-connected-grid">
    <div className="integration-stat">
    <span className="integration-stat-label">
      Año lectivo
  </span>

  <strong>
  {academicYear ?? '—'}
    </strong>
    </div>

    <div className="integration-stat">
  <span className="integration-stat-label">
  Escala
  </span>

  <strong>
  {baseScore !== null
    ? `0 – ${baseScore}`
    : '—'}
    </strong>
    </div>

    <label className="form-field period-field">
    <span>
      Período a sincronizar
  </span>

  <select
    value={selectedPeriodId}
    disabled={syncing}
    onChange={(event) =>
    onPeriodChange(
      event.target.value,
    )
  }
  >
    {periods.map((period) => (
      <option
        key={period.id}
      value={period.id}
        >
        {period.abbreviation}
      {' · '}
      {period.name}
      </option>
    ))}
    </select>
    </label>

    <div className="period-ready">
  <span className="period-ready-label">
  Seleccionado
  </span>

  <strong>
  {selectedPeriod
    ? selectedPeriod.name
    : 'Sin período'}
    </strong>

    <span>
    Listo para sincronizar
  </span>
  </div>
  </div>

  <div className="idukay-sync-actions">
  <button
    className="btn btn-primary"
    disabled={
        syncing ||
      !selectedPeriodId
  }
    onClick={onSync}
    >
    {syncing
      ? 'Sincronizando...'
      : `Sincronizar ${
        selectedPeriod?.abbreviation ??
        'período'
      }`}
    </button>

    {syncResult && (
      <IdukaySyncResult
        result={syncResult}
      />
    )}
    </div>
    </>
  )}
  </section>
);
}

function ConnectionStatus({
                            connected,
                          }: {
  connected: boolean;
}) {
  return (
    <span
      className={
      connected
      ? 'connection-status connection-status-ok'
      : 'connection-status connection-status-off'
}
>
  <span className="connection-status-dot" />

  {connected
    ? 'Conectado'
    : 'Sin conectar'}
  </span>
);
}

function IdukaySyncResult({
                            result,
                          }: {
  result: SyncIdukayPeriodResponse;
}) {
  return (
    <div className="sync-result-grid">
    <SyncMetric
      label="Cursos"
  value={result.coursesProcessed}
  />

  <SyncMetric
  label="Calificaciones"
  value={result.gradesProcessed}
  />

  <SyncMetric
  label="Alertas"
  value={result.openAlerts}
  />

  <SyncMetric
  label="Avisos"
  value={result.warnings}
  />

  <SyncMetric
  label="Críticos"
  value={result.critical}
  />
  </div>
);
}

function SyncMetric({
                      label,
                      value,
                    }: {
  label: string;
  value: number;
}) {
  return (
    <div>
      <span>{label}</span>
    <strong>{value}</strong>
    </div>
  );
}
