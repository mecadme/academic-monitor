export type AcademicStatus =
  | 'OK'
  | 'WARNING'
  | 'CRITICAL';

type StatusBadgeProps = {
  status: AcademicStatus;
};

const labels: Record<
  AcademicStatus,
  string
> = {
  OK: 'Bien',
  WARNING: 'Aviso',
  CRITICAL: 'Crítico',
};

const classNames: Record<
  AcademicStatus,
  string
> = {
  OK: 'status-badge status-ok',
  WARNING:
    'status-badge status-warning',
  CRITICAL:
    'status-badge status-critical',
};

export function StatusBadge({
                              status,
                            }: StatusBadgeProps) {
  return (
    <span
      className={
        classNames[status]
      }
    >
      <span className="status-dot" />

      {labels[status]}
    </span>
  );
}
