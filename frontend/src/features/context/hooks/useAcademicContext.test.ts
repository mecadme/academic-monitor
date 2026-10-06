import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAcademicContext } from './useAcademicContext';

const session = { user: { id: 'teacher', email: 'teacher@example.com', systemRole: 'USER' }, institution: { id: 'school', name: 'School', role: 'TEACHER' } };

describe('useAcademicContext', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('initializes CSRF and restores authenticated identity with /me', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 204 })).mockResolvedValueOnce(Response.json(session));
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() => useAcademicContext());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(fetchMock.mock.calls.map(([url]) => new URL(url).pathname)).toEqual(['/api/v1/auth/csrf', '/api/v1/auth/me']);
    expect(result.current.institutionId).toBe('school');
    expect(result.current.teacherUserId).toBe('teacher');
    expect(result.current.session).toEqual(session);
    expect(result.current.error).toBeNull();
  });

  it('reports a connection failure without pretending authentication expired', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const { result } = renderHook(() => useAcademicContext());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.session).toBeNull();
    expect(result.current.error).toContain('No se pudo verificar');
  });
});
