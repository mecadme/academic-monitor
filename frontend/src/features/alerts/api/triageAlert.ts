import { apiFetch } from '../../../api/apiFetch';

export type TriageAlertInput = {
  alertId: string;
};


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
  }: TriageAlertInput,
  command: 'acknowledge' | 'mark-pending',
) {
  await apiFetch(
    `/api/v1/alerts/${encodeURIComponent(alertId)}/${command}`,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
      },
    },
    { errorMessage: (status) => `No se pudo actualizar la atención de la alerta (${status}).` },
  );

}
