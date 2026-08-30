type AppHeaderProps = {
  syncing: boolean;
  onInitialSync: () => void;
  onImprovement: () => void;
};

export function AppHeader({
                            syncing,
                            onInitialSync,
                            onImprovement,
                          }: AppHeaderProps) {
  return (
    <header className="topbar">
      <div className="container topbar-content">
        <div className="brand-block">
          <div className="brand-heading">
            <div className="brand-logo">
              AM
            </div>

            <div>
              <div className="brand-meta">
                <span className="demo-badge">
                  DEMO
                </span>

                <span>
                  Seguimiento académico
                </span>
              </div>

              <h1 className="brand-title">
                Academic Monitor
              </h1>
            </div>
          </div>

          <p className="brand-subtitle">
            Alertas académicas claras para
            intervenir antes de que un problema
            crezca.
          </p>
        </div>

        <div className="actions">
          <button
            className="btn btn-secondary"
            disabled={syncing}
            onClick={onInitialSync}
          >
            {syncing
              ? 'Procesando...'
              : 'Sincronizar demo'}
          </button>

          <button
            className="btn btn-primary"
            disabled={syncing}
            onClick={onImprovement}
          >
            {syncing
              ? 'Procesando...'
              : 'Simular mejora'}
          </button>
        </div>
      </div>
    </header>
  );
}
