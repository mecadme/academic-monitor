import { Activity, BrainCircuit, Database, RefreshCw, Server, WifiOff } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { fetchSystemHealth } from './api';
import type { ServiceStatus, SystemHealth } from './types';

const serviceLabels: Record<string, string> = {
  frontend: 'Frontend',
  backend: 'Backend',
  database: 'Database',
  ai: 'AI Service',
};

function App() {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const frontendStatus: ServiceStatus = useMemo(
    () => ({ name: 'frontend', status: 'UP', message: 'React app is running' }),
    [],
  );

  async function loadHealth() {
    setIsLoading(true);
    setError(null);

    try {
      const response = await fetchSystemHealth();
      setHealth(response);
    } catch (currentError) {
      setHealth(null);
      setError(currentError instanceof Error ? currentError.message : 'Backend is not reachable');
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadHealth();
  }, []);

  const services = [frontendStatus, ...(health?.services ?? fallbackBackendServices(error))];
  const systemStatus = error ? 'DEGRADED' : (health?.status ?? 'DEGRADED');

  return (
    <main className="app-shell">
      <section className="status-panel" aria-labelledby="status-title">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Academic Monitor</p>
            <h1 id="status-title">Estado del sistema</h1>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={loadHealth}
            title="Actualizar estado"
            aria-label="Actualizar estado"
          >
            <RefreshCw size={18} aria-hidden="true" />
          </button>
        </div>

        <div className={`summary summary-${systemStatus.toLowerCase()}`}>
          {systemStatus === 'UP' ? (
            <Activity size={20} aria-hidden="true" />
          ) : (
            <WifiOff size={20} aria-hidden="true" />
          )}
          <span>{summaryText(isLoading, systemStatus)}</span>
        </div>

        <div className="service-list" aria-live="polite">
          {services.map((service) => (
            <article className="service-row" key={service.name}>
              <div className="service-icon" aria-hidden="true">
                {iconFor(service.name)}
              </div>
              <div className="service-copy">
                <h2>{serviceLabels[service.name] ?? service.name}</h2>
                <p>{service.message}</p>
              </div>
              <span className={`status-pill status-${service.status.toLowerCase()}`}>
                {service.status === 'UP' ? 'Online' : 'Revisar'}
              </span>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}

function summaryText(isLoading: boolean, status: string) {
  if (isLoading) {
    return 'Comprobando servicios';
  }

  return status === 'UP' ? 'Todos los servicios estan online' : 'Hay servicios por revisar';
}

function fallbackBackendServices(error: string | null): ServiceStatus[] {
  const message = error ? 'Backend is not reachable yet' : 'Waiting for backend response';

  return [
    { name: 'backend', status: 'DEGRADED', message },
    { name: 'database', status: 'DEGRADED', message: 'Waiting for backend health check' },
    { name: 'ai', status: 'DEGRADED', message: 'Waiting for backend health check' },
  ];
}

function iconFor(serviceName: string) {
  if (serviceName === 'database') {
    return <Database size={20} />;
  }

  if (serviceName === 'ai') {
    return <BrainCircuit size={20} />;
  }

  return <Server size={20} />;
}

export default App;
