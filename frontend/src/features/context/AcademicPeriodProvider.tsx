import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import type { AcademicPeriod } from '../alerts/api/fetchAcademicPeriods';
import { useAcademicPeriods } from '../alerts/hooks/useAcademicPeriods';

export type AcademicYear = {
  id: string;
  name: string;
};

type AcademicPeriodContextValue = {
  periods: AcademicPeriod[];
  academicYears: AcademicYear[];
  selectedAcademicYear: AcademicYear | null;
  selectedAcademicYearId: string | null;
  selectedPeriod: AcademicPeriod | null;
  selectedPeriodId: string | null;
  loading: boolean;
  error: string | null;
  selectAcademicYear: (academicYearId: string) => void;
  selectPeriod: (periodId: string) => void;
  refresh: () => Promise<void>;
};

const AcademicPeriodContext = createContext<AcademicPeriodContextValue | null>(null);

type Props = {
  institutionId: string | null;
  teacherUserId: string | null;
  children: ReactNode;
};

export function AcademicPeriodProvider({ institutionId, teacherUserId, children }: Props) {
  const catalog = useAcademicPeriods({ institutionId, teacherUserId });
  const [selectedAcademicYearId, setSelectedAcademicYearId] = useState<string | null>(null);
  const [selectedPeriodId, setSelectedPeriodId] = useState<string | null>(null);
  const storagePrefix = institutionId && teacherUserId
    ? `academic-monitor:academic-context:${institutionId}:${teacherUserId}`
    : null;
  const periods = catalog.catalog?.periods ?? [];
  const academicYears = useMemo<AcademicYear[]>(() => {
    const years = new Map<string, AcademicYear>();
    periods.forEach((period) => {
      if (!years.has(period.academicYearId)) {
        years.set(period.academicYearId, { id: period.academicYearId, name: period.academicYear ?? 'Año lectivo' });
      }
    });
    return [...years.values()].sort((a, b) => b.name.localeCompare(a.name));
  }, [periods]);
  const periodsForSelectedYear = useMemo(
    () => periods.filter((period) => period.academicYearId === selectedAcademicYearId),
    [periods, selectedAcademicYearId],
  );

  useEffect(() => {
    if (!storagePrefix || !catalog.catalog) return;
    const saved = window.localStorage.getItem(`${storagePrefix}:year`);
    setSelectedAcademicYearId((current) =>
      current && academicYears.some((year) => year.id === current)
        ? current
        : academicYears.some((year) => year.id === saved)
          ? saved
          : academicYears[0]?.id ?? null,
    );
  }, [academicYears, catalog.catalog, storagePrefix]);

  useEffect(() => {
    if (!storagePrefix || !selectedAcademicYearId) {
      setSelectedPeriodId(null);
      return;
    }
    const saved = window.localStorage.getItem(`${storagePrefix}:period:${selectedAcademicYearId}`);
    setSelectedPeriodId((current) => {
      if (current && periodsForSelectedYear.some((period) => period.id === current)) return current;
      const validSaved = saved && periodsForSelectedYear.some((period) => period.id === saved) ? saved : null;
      return validSaved ?? [...periodsForSelectedYear].sort((a, b) => b.order - a.order)
        .find((period) => period.synchronized)?.id ?? periodsForSelectedYear[0]?.id ?? null;
    });
  }, [periodsForSelectedYear, selectedAcademicYearId, storagePrefix]);

  const value = useMemo<AcademicPeriodContextValue>(() => ({
    periods: periodsForSelectedYear,
    academicYears,
    selectedAcademicYear: academicYears.find((year) => year.id === selectedAcademicYearId) ?? null,
    selectedAcademicYearId,
    selectedPeriod: periodsForSelectedYear.find((period) => period.id === selectedPeriodId) ?? null,
    selectedPeriodId,
    loading: catalog.loading,
    error: catalog.error,
    selectAcademicYear: (academicYearId) => {
      if (!academicYears.some((year) => year.id === academicYearId)) return;
      const periodsForYear = periods.filter((period) => period.academicYearId === academicYearId);
      const savedPeriodId = storagePrefix
        ? window.localStorage.getItem(`${storagePrefix}:period:${academicYearId}`)
        : null;
      const validSavedPeriodId = savedPeriodId && periodsForYear.some((period) => period.id === savedPeriodId)
        ? savedPeriodId
        : null;
      const fallbackPeriodId = [...periodsForYear].sort((a, b) => b.order - a.order)
        .find((period) => period.synchronized)?.id ?? periodsForYear[0]?.id ?? null;
      setSelectedAcademicYearId(academicYearId);
      setSelectedPeriodId(validSavedPeriodId ?? fallbackPeriodId);
      if (storagePrefix) window.localStorage.setItem(`${storagePrefix}:year`, academicYearId);
    },
    selectPeriod: (periodId) => {
      if (!periodsForSelectedYear.some((period) => period.id === periodId)) return;
      setSelectedPeriodId(periodId);
      if (storagePrefix && selectedAcademicYearId) window.localStorage.setItem(`${storagePrefix}:period:${selectedAcademicYearId}`, periodId);
    },
    refresh: async () => {
      await catalog.refresh();
    },
  }), [academicYears, catalog.error, catalog.loading, catalog.refresh, periodsForSelectedYear, selectedAcademicYearId, selectedPeriodId, storagePrefix]);

  return <AcademicPeriodContext.Provider value={value}>{children}</AcademicPeriodContext.Provider>;
}

// The hook intentionally lives beside its provider to keep the period contract cohesive.
// eslint-disable-next-line react-refresh/only-export-components
export function useAcademicPeriod() {
  const context = useContext(AcademicPeriodContext);
  if (!context) throw new Error('useAcademicPeriod must be used within AcademicPeriodProvider');
  return context;
}
