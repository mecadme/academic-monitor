import type {
  AcademicDashboardCourse,
} from '../api/fetchAcademicDashboard';

type AcademicCoursePanelProps = {
  courses: AcademicDashboardCourse[];
  refreshing: boolean;
  onRefresh: () => void;
};

export function AcademicCoursePanel({
  courses,
  refreshing,
  onRefresh,
}: AcademicCoursePanelProps) {
  return (
    <section className="container panel">
      <div className="panel-header">
        <div>
          <p className="section-label">
            Cursos del docente
          </p>

          <h3 className="panel-title">
            Resumen por curso
          </h3>
        </div>

        <button
          className="btn btn-ghost"
          disabled={refreshing}
          onClick={onRefresh}
        >
          {refreshing
            ? 'Actualizando...'
            : 'Actualizar vista'}
        </button>
      </div>

      {courses.length === 0 ? (
        <p className="empty-dashboard-message">
          Aún no hay cursos sincronizados para
          este contexto académico.
        </p>
      ) : (
        <div className="table-wrapper">
          <table className="dashboard-table">
            <thead>
              <tr>
                <th>Curso</th>
                <th>Materia</th>
                <th>Año lectivo</th>
                <th>Estudiantes</th>
                <th>Actividades</th>
                <th>Alertas</th>
                <th>Avisos</th>
                <th>Críticos</th>
              </tr>
            </thead>

            <tbody>
              {courses.map((course) => (
                <tr key={course.id}>
                  <td className="student-name">
                    {course.name}
                  </td>
                  <td>{course.subject ?? '—'}</td>
                  <td>{course.academicYear ?? '—'}</td>
                  <td>{course.students}</td>
                  <td>{course.activities}</td>
                  <td>{course.openAlerts}</td>
                  <td>{course.warnings}</td>
                  <td>{course.critical}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
