import { Bell, BookOpen, ChevronRight, Home, Menu, MessageSquare, Settings, TriangleAlert } from 'lucide-react';
import { useState, type ReactNode } from 'react';

import { useAcademicPeriod } from '../../features/context/AcademicPeriodProvider';

type Props = { path: string; onNavigate: (path: string) => void; connected?: boolean; notificationCenter?: ReactNode; children: ReactNode };

const navigation = [
  { path: '/', label: 'Inicio', icon: Home },
  { path: '/courses', label: 'Cursos', icon: BookOpen },
  { path: '/alerts', label: 'Alertas', icon: TriangleAlert },
  { path: '/communications', label: 'Comunicaciones', icon: MessageSquare },
  { path: '/settings', label: 'Configuración', icon: Settings },
];

export function AppShell({ path, onNavigate, connected = false, notificationCenter, children }: Props) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { academicYears, periods, selectedAcademicYearId, selectedPeriodId, selectAcademicYear, selectPeriod, loading } = useAcademicPeriod();
  const isActive = (itemPath: string) => itemPath === '/' ? path === '/' : path.startsWith(itemPath);
  const navigate = (target: string) => { setMobileOpen(false); onNavigate(target); };

  return <div className="app-frame">
    <aside className={`app-sidebar ${mobileOpen ? 'is-open' : ''}`} aria-label="Navegación principal">
      <div className="sidebar-brand"><span className="brand-logo">AM</span><span>Academic Monitor</span></div>
      <nav>
        {navigation.map(({ path: itemPath, label, icon: Icon }) => <button key={itemPath} type="button" className={`nav-item ${isActive(itemPath) ? 'is-active' : ''}`} aria-current={isActive(itemPath) ? 'page' : undefined} onClick={() => navigate(itemPath)}><Icon size={19} />{label}</button>)}
        {path.startsWith('/settings') && <button type="button" className={`nav-subitem ${path === '/settings/integrations' ? 'is-active' : ''}`} onClick={() => navigate('/settings/integrations')}><ChevronRight size={15} />Integraciones</button>}
      </nav>
    </aside>
    {mobileOpen && <button aria-label="Cerrar navegación" className="sidebar-backdrop" onClick={() => setMobileOpen(false)} />}
    <div className="app-main">
      <header className="app-topbar">
        <button className="mobile-menu" type="button" aria-label="Abrir navegación" onClick={() => setMobileOpen(true)}><Menu /></button>
        <div className="period-controls">
          <div className="academic-context-control">
            <label htmlFor="global-academic-year">Año lectivo</label>
            {academicYears.length > 1 ? <select id="global-academic-year" value={selectedAcademicYearId ?? ''} disabled={loading || academicYears.length === 0} onChange={(event) => selectAcademicYear(event.target.value)}>{academicYears.map((year) => <option key={year.id} value={year.id}>{year.name}</option>)}</select> : <span className="academic-year">{academicYears[0]?.name ?? 'Año lectivo'}</span>}
          </div>
          <div className="academic-context-control">
            <label htmlFor="global-period">Período</label>
            <select id="global-period" value={selectedPeriodId ?? ''} disabled={loading || periods.length === 0} onChange={(event) => selectPeriod(event.target.value)}>{periods.map((period) => <option key={period.id} value={period.id}>{period.abbreviation ? `${period.abbreviation} · ` : ''}{period.name}</option>)}</select>
          </div>
        </div>
        <div className="topbar-actions">{notificationCenter ?? <button className="notification-button" type="button" aria-label="Notificaciones"><Bell size={19} /></button>}<button className="connection-link" type="button" onClick={() => navigate('/settings/integrations')}><span className={`connection-dot ${connected ? 'is-connected' : ''}`} />{connected ? 'Conectado' : 'Conectar'}</button></div>
      </header>
      <main className="route-content">{children}</main>
    </div>
  </div>;
}
