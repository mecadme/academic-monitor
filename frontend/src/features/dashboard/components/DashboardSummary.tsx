import type { DashboardResult } from '../../../api/demo';
import { SummaryCard } from './SummaryCard';

type DashboardSummaryProps = {
  summary: DashboardResult['summary'];
};

export function DashboardSummary({
                                   summary,
                                 }: DashboardSummaryProps) {
  return (
    <section className="container summary-grid">
      <SummaryCard
        label="Estudiantes"
        value={summary.totalStudents}
        helper="Curso actual"
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
