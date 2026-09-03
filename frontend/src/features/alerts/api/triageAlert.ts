export type TriageAlertInput = {
  alertId: string;
  institutionId: string;
  teacherUserId: string;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080';

export async function acknowledgeAlert(
  input: TriageAlertInput,
) {
  await postTriageCommand(input, 'acknowledge');
}

export async function markAlertPending(
  input: TriageAlertInput,
) {
  await postTriageCommand(input, 'mark-pending');
}

async function postTriageCommand(
  {
    alertId,
    institutionId,
    teacherUserId,
  }: TriageAlertInput,
  command: 'acknowledge' | 'mark-pending',
) {
  const query = new URLSearchParams({
    institutionId,
    teacherUserId,
  });
  const response = await fetch(
    `${apiBaseUrl}/api/v1/alerts/${encodeURIComponent(alertId)}/${command}?${query.toString()}`,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
      },
    },
  );

  if (!response.ok) {
    throw new Error(
      `No se pudo actualizar la atención de la alerta (${response.status}).`,
    );
  }
}
