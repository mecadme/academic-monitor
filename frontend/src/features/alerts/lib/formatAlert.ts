const dueDateFormatter = new Intl.DateTimeFormat(
  'es-EC',
  {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  },
);

const acknowledgedAtFormatter = new Intl.DateTimeFormat(
  'es-EC',
  {
    dateStyle: 'medium',
    timeStyle: 'short',
  },
);

export function formatAlertScore(
  value: number,
) {
  return Number.isFinite(value)
    ? value.toFixed(2)
    : '—';
}

export function formatAlertDueDate(
  dueDate: string,
) {
  const date = new Date(`${dueDate}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return dueDate;
  }

  return dueDateFormatter.format(date);
}

export function formatAcknowledgedAt(
  acknowledgedAt: string,
) {
  const date = new Date(acknowledgedAt);

  if (Number.isNaN(date.getTime())) {
    return acknowledgedAt;
  }

  return acknowledgedAtFormatter.format(date);
}
