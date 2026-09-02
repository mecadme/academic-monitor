export type AcademicContext = {
  institutionId: string;
  teacherUserId: string;
};

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ??
  'http://localhost:8080';

export async function bootstrapAcademicContext(): Promise<AcademicContext> {
  const response = await fetch(
    `${apiBaseUrl}/api/v1/context/bootstrap`,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
      },
    },
  );

  if (!response.ok) {
    throw new Error(
      `No se pudo inicializar el contexto académico (${response.status}).`,
    );
  }

  return response.json();
}
