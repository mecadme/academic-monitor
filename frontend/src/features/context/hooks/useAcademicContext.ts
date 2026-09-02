import { useEffect, useState } from 'react';

import { bootstrapAcademicContext } from '../api/bootstrapAcademicContext';

export function useAcademicContext() {
  const [institutionId, setInstitutionId] =
    useState<string | null>(null);

  const [teacherUserId, setTeacherUserId] =
    useState<string | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function bootstrap() {
      try {
        const context =
          await bootstrapAcademicContext();

        if (!active) {
          return;
        }

        setInstitutionId(
          context.institutionId,
        );

        setTeacherUserId(
          context.teacherUserId,
        );
      } catch (err) {
        if (!active) {
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : 'No se pudo inicializar el contexto académico.',
        );
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    bootstrap();

    return () => {
      active = false;
    };
  }, []);

  return {
    institutionId,
    teacherUserId,
    loading,
    error,
  };
}
