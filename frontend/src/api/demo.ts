import { apiFetch } from './apiFetch';

export type DemoScenario = "INITIAL" | "IMPROVED";

export interface SyncResult {
  institutionId: string;
  teacherUserId: string;
  courseId: string;
  courseName: string;
  scenario: DemoScenario;
  students: number;
  gradesProcessed: number;
  openAlerts: number;
  warnings: number;
  critical: number;
}

export interface DashboardResult {
  courseId: string;
  courseName: string;
  subject: string;

  activity: {
    id: string;
    name: string;
  };

  summary: {
    totalStudents: number;
    openAlerts: number;
    warnings: number;
    critical: number;
    resolvedAlerts: number;
  };

  students: Array<{
    id: string;
    name: string;
    score: number;
    status: "OK" | "WARNING" | "CRITICAL";
  }>;
}


export async function syncDemo(
  scenario: DemoScenario,
): Promise<SyncResult> {
  const response = await apiFetch(
    `/api/v1/demo/sync?scenario=${scenario}`,
    {
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error("No se pudo sincronizar la información.");
  }

  return response.json();
}

export async function getDashboard(
): Promise<DashboardResult> {
  const response = await apiFetch(
    `/api/v1/demo/dashboard`,
  );

  if (!response.ok) {
    throw new Error("No se pudo cargar el dashboard.");
  }

  return response.json();
}
