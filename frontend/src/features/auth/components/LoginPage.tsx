import { useState, type FormEvent } from 'react';
import { ApiError, type ApiProblem } from '../../../api/apiFetch';
import type { LoginInput } from '../api/auth';

type Props = { onLogin: (input: LoginInput) => Promise<void>; initialError?: string | null };

export function LoginPage({ onLogin, initialError }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [institutions, setInstitutions] = useState<ApiProblem['institutions']>([]);
  const [institutionId, setInstitutionId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(initialError ?? null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await onLogin({ email: email.trim(), password, ...(institutionId ? { institutionId } : {}) });
      setPassword('');
    } catch (err) {
      if (err instanceof ApiError && err.problem.code === 'INSTITUTION_SELECTION_REQUIRED') {
        setInstitutions(err.problem.institutions ?? []);
      } else {
        setPassword('');
        setError(err instanceof ApiError && err.status === 401 ? 'Correo o contraseña incorrectos.' : 'No se pudo iniciar sesión. Inténtalo nuevamente.');
      }
    } finally { setBusy(false); }
  }

  return <main className="login-page">
    <section className="login-card" aria-labelledby="login-title">
      <div className="login-brand"><span className="brand-logo" aria-hidden="true">AM</span><span>Academic Monitor</span></div>
      <h1 id="login-title">Iniciar sesión</h1>
      <p>Accede al seguimiento académico de tu institución.</p>
      <form onSubmit={(event) => void submit(event)} aria-busy={busy}>
        <label htmlFor="login-email">Correo electrónico</label>
        <input id="login-email" name="email" type="email" autoComplete="email" required value={email} disabled={busy} onChange={(event) => { setEmail(event.target.value); setInstitutions([]); setInstitutionId(''); }} />
        <label htmlFor="login-password">Contraseña</label>
        <input id="login-password" name="password" type="password" autoComplete="current-password" required value={password} disabled={busy} onChange={(event) => setPassword(event.target.value)} />
        {!!institutions?.length && <>
          <p className="login-selection" role="status">Selecciona la institución con la que deseas continuar.</p>
          <label htmlFor="login-institution">Institución</label>
          <select id="login-institution" required value={institutionId} disabled={busy} onChange={(event) => setInstitutionId(event.target.value)}>
            <option value="">Selecciona una institución</option>
            {institutions.map((institution) => <option key={institution.institutionId} value={institution.institutionId}>{institution.institutionName} · {institution.institutionRole === 'TEACHER' ? 'Docente' : 'Administrador'}</option>)}
          </select>
        </>}
        {error && <p className="login-error" role="alert">{error}</p>}
        <button className="btn btn-primary" type="submit" disabled={busy}>{busy ? 'Iniciando sesión…' : 'Iniciar sesión'}</button>
      </form>
    </section>
  </main>;
}
