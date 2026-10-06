import { apiFetch } from '../../../api/apiFetch';

export type AuthSession = {
  user: { id: string; email: string; systemRole: 'USER' | 'SUPER_ADMIN' };
  institution: { id: string; name: string; role: 'ADMIN' | 'TEACHER' };
};
export type LoginInput = { email: string; password: string; institutionId?: string };

export async function fetchSession(): Promise<AuthSession> {
  return (await apiFetch('/api/v1/auth/me')).json();
}

export async function login(input: LoginInput): Promise<AuthSession> {
  return (await apiFetch('/api/v1/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  }, { refresh: false, errorMessage: 'Correo o contraseña incorrectos.' })).json();
}

export async function logout(): Promise<void> {
  await apiFetch('/api/v1/auth/logout', { method: 'POST' }, { refresh: false });
}
