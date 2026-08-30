import type { DashboardResult } from '../../../api/demo';

type DashboardHeroProps = {
  dashboard: DashboardResult;
};

export function DashboardHero({
                                dashboard,
                              }: DashboardHeroProps) {
  return (
    <section className="container hero">
      <div className="hero-text">
        <p className="section-label">
          Curso monitoreado
        </p>

        <h2 className="course-title">
          {dashboard.courseName}
        </h2>

        <p className="course-subtitle">
          {dashboard.subject}
        </p>
      </div>

      <div className="hero-note">
        <p className="hero-note-title">
          Regla de monitoreo
        </p>

        <div className="rule-row">
          <span className="rule-dot rule-critical" />

          <span>
            <strong>Crítico</strong>
            {' · '}
            nota ≤ 5.00
          </span>
        </div>

        <div className="rule-row">
          <span className="rule-dot rule-warning" />

          <span>
            <strong>Aviso</strong>
            {' · '}
            nota &gt; 5.00 y ≤ 7.00
          </span>
        </div>
      </div>
    </section>
  );
}
