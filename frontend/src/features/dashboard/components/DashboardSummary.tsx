import type { AcademicDashboard } from '../api/fetchAcademicDashboard';
import { SummaryCard } from './SummaryCard';

type DashboardSummaryProps = {
  summary: AcademicDashboard['summary'];
};

export function DashboardSummary({
                                   summary,
                                 }: DashboardSummaryProps) {
  return (
    <section className="container summary-grid">
      <SummaryCard
        label="Cursos"
        value={summary.courses}
        helper="Asignados al docente"
      />

      <SummaryCard
        label="Estudiantes"
        value={summary.students}
        helper="Sin duplicados"
      />

      <SummaryCard
        label="Actividades"
        value={summary.activities}
        helper="En todos los cursos"
      />

      <SummaryCard
        label="Alertas abiertas"
        value={summary.openAlerts}
        helper="Requieren revisión"
      />

      <SummaryCard
        label="Avisos"
        value={summary.warnings}
        helper="Seguimiento"
      />

      <SummaryCard
        label="Críticos"
        value={summary.critical}
        helper="Atención prioritaria"
      />
    </section>
  );
}
