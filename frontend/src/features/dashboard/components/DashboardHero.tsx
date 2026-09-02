import type { AcademicDashboard } from '../api/fetchAcademicDashboard';

type DashboardHeroProps = {
  dashboard: AcademicDashboard;
};

export function DashboardHero({
                                dashboard,
                              }: DashboardHeroProps) {
  return (
    <section className="container hero">
      <div className="hero-text">
        <p className="section-label">
          Dashboard académico
        </p>

        <h2 className="course-title">
          Seguimiento de cursos
        </h2>

        <p className="course-subtitle">
          Información académica persistida y
          alertas abiertas del contexto actual.
        </p>
      </div>

      <div className="hero-note">
        <p className="hero-note-title">
          Alcance actual
        </p>

        <div className="rule-row">
          <span className="rule-dot rule-warning" />

          <span>
            <strong>{dashboard.summary.courses}</strong>
            {' · '}
            cursos del docente
          </span>
        </div>

        <div className="rule-row">
          <span className="rule-dot rule-warning" />

          <span>
            <strong>{dashboard.summary.students}</strong>
            {' · '}
            estudiantes distintos
          </span>
        </div>
      </div>
    </section>
  );
}
