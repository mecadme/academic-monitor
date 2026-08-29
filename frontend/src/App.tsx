import { useEffect, useState } from 'react';
import './App.css';
import { type DashboardResult, getDashboard, syncDemo } from './api/demo';
import { testIdukayLogin } from './features/idukay/api/testIdukayLogin';

function App() {
  const [dashboard, setDashboard] = useState<DashboardResult | null>(null);

  const [teacherUserId, setTeacherUserId] = useState<string | null>(null);
  const [institutionId, setInstitutionId] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  async function loadScenario(scenario: 'INITIAL' | 'IMPROVED') {
    try {
      setSyncing(true);
      setError(null);

      const sync = await syncDemo(scenario);

      setInstitutionId(sync.institutionId);
      setTeacherUserId(sync.teacherUserId);

      const data = await getDashboard(sync.teacherUserId);
      setDashboard(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Ocurrió un error inesperado.',
      );
    } finally {
      setSyncing(false);
      setLoading(false);
    }
  }

  async function testRealIdukayLogin() {
    if (!institutionId || !teacherUserId) {
      setError('No hay contexto académico disponible.');
      return;
    }

    try {
      setError(null);

      const result = await testIdukayLogin({
        email,
        password,
        institutionId,
        teacherUserId,
      });

      console.log(result);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'No se pudo conectar con Idukay.',
      );
    }
  }

  async function refreshDashboard() {
    try {
      if (!teacherUserId) {
        return;
      }

      setError(null);

      const data = await getDashboard(teacherUserId);

      setDashboard(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'No se pudo actualizar el dashboard.',
      );
    }
  }

  useEffect(() => {
    loadScenario('INITIAL');
  }, []);

  if (loading) {
    return (
      <main className="app-shell loading-shell">
        <div className="loading-card">
          <h1>Academic Monitor</h1>
          <p>Preparando entorno de demostración...</p>
        </div>
      </main>
    );
  }

  if (error && !dashboard) {
    return (
      <main className="app-shell loading-shell">
        <div className="loading-card">
          <h1>No se pudo cargar Academic Monitor</h1>
          <p>{error}</p>

          <button
            className="btn btn-primary"
            onClick={() => loadScenario('INITIAL')}
          >
            Reintentar
          </button>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return null;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="container topbar-content">
          <div>
            <p className="eyebrow">MODO DEMO</p>
            <h1 className="brand-title">Academic Monitor</h1>
            <p className="brand-subtitle">
              Monitoreo automático de alertas académicas
            </p>
          </div>

          <div className="actions">
            <button
              className="btn btn-secondary"
              disabled={syncing}
              onClick={() => loadScenario('INITIAL')}
            >
              {syncing ? 'Procesando...' : 'Sincronizar'}
            </button>

            <button
              className="btn btn-primary"
              disabled={syncing}
              onClick={() => loadScenario('IMPROVED')}
            >
              {syncing ? 'Procesando...' : 'Simular mejora'}
            </button>
          </div>
        </div>
      </header>

      <section className="container hero">
        <div className="hero-text">
          <p className="section-label">Curso monitoreado</p>
          <h2 className="course-title">{dashboard.courseName}</h2>
          <p className="course-subtitle">{dashboard.subject}</p>
        </div>

        <div className="hero-note">
          <p className="hero-note-title">Regla de alerta</p>

          <p>
            <strong>Crítico:</strong> nota ≤ 5.00
          </p>

          <p>
            <strong>Aviso:</strong> nota {'>'} 5.00 y ≤ 7.00
          </p>
        </div>
      </section>

      {error && (
        <section className="container">
          <div className="error-banner">{error}</div>
        </section>
      )}

      <section className="container summary-grid">
        <SummaryCard
          label="Estudiantes"
          value={dashboard.summary.totalStudents}
        />

        <SummaryCard
          label="Alertas abiertas"
          value={dashboard.summary.openAlerts}
        />

        <SummaryCard
          label="Avisos"
          value={dashboard.summary.warnings}
        />

        <SummaryCard
          label="Críticos"
          value={dashboard.summary.critical}
        />
      </section>

      <section className="container panel">
        <div className="panel-header">
          <div>
            <p className="section-label">Actividad analizada</p>
            <h3 className="panel-title">
              {dashboard.activity.name}
            </h3>
          </div>

          <button
            className="btn btn-ghost"
            onClick={refreshDashboard}
          >
            Actualizar vista
          </button>

          <input
            type="email"
            value={email}
            placeholder="Correo de Idukay"
            onChange={(event) =>
              setEmail(event.target.value)
            }
          />

          <input
            type="password"
            value={password}
            placeholder="Contraseña de Idukay"
            onChange={(event) =>
              setPassword(event.target.value)
            }
          />

          <button onClick={testRealIdukayLogin}>
            Probar conexión Idukay
          </button>
        </div>

        <div className="table-wrapper">
          <table className="dashboard-table">
            <thead>
            <tr>
              <th>Estudiante</th>
              <th>Nota</th>
              <th>Estado</th>
            </tr>
            </thead>

            <tbody>
            {dashboard.students.map((student) => (
              <tr key={student.id}>
                <td className="student-name">
                  {student.name}
                </td>

                <td className="student-score">
                  {Number(student.score).toLocaleString(
                    'es-EC',
                    {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    },
                  )}
                </td>

                <td>
                  <StatusBadge status={student.status} />
                </td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="container footer-note">
        <div className="footer-note-card">
          <strong>Escenario actual:</strong> Use “Sincronizar” para cargar el
          escenario base y “Simular mejora” para mostrar la resolución
          automática de alertas.
        </div>
      </section>
    </main>
  );
}

function SummaryCard({
                       label,
                       value,
                     }: {
  label: string;
  value: number;
}) {
  return (
    <article className="summary-card">
      <p className="summary-label">{label}</p>
      <p className="summary-value">{value}</p>
    </article>
  );
}

function StatusBadge({
                       status,
                     }: {
  status: 'OK' | 'WARNING' | 'CRITICAL';
}) {
  const labels = {
    OK: 'Bien',
    WARNING: 'Aviso',
    CRITICAL: 'Crítico',
  };

  const className = {
    OK: 'status-badge status-ok',
    WARNING: 'status-badge status-warning',
    CRITICAL: 'status-badge status-critical',
  };

  return (
    <span className={className[status]}>
      {labels[status]}
    </span>
  );
}

export default App;
