import type { DashboardResult } from '../../../api/demo';
import { StatusBadge } from './StatusBadge';

type ActivityPanelProps = {
  dashboard: DashboardResult;
  onRefresh: () => void;
};

export function ActivityPanel({
                                dashboard,
                                onRefresh,
                              }: ActivityPanelProps) {
  return (
    <section className="container panel">
      <div className="panel-header">
        <div>
          <p className="section-label">
            Actividad analizada
          </p>

          <h3 className="panel-title">
            {dashboard.activity.name}
          </h3>
        </div>

        <button
          className="btn btn-ghost"
          onClick={onRefresh}
        >
          Actualizar vista
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
          {dashboard.students.map(
            (student) => (
              <tr key={student.id}>
                <td className="student-name">
                  {student.name}
                </td>

                <td className="student-score">
                  {Number(
                    student.score,
                  ).toLocaleString(
                    'es-EC',
                    {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    },
                  )}
                </td>

                <td>
                  <StatusBadge
                    status={student.status}
                  />
                </td>
              </tr>
            ),
          )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
