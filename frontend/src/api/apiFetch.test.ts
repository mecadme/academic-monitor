import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiFetch, ApiError, sessionExpiredEvent } from './apiFetch';

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('apiFetch', () => {
  it.each(['POST', 'PATCH', 'DELETE'])('includes cookies and the CSRF header on %s', async (method) => {
    document.cookie = 'XSRF-TOKEN=csrf%2Bvalue; Path=/';
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);
    await apiFetch('/api/v1/resource', { method });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/v1/resource', expect.objectContaining({
      method, credentials: 'include', headers: expect.objectContaining({ 'x-xsrf-token': 'csrf+value' }),
    }));
  });

  it('initializes a missing CSRF cookie before a mutable request', async () => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    const fetchMock = vi.fn((url: string) => {
      if (url.endsWith('/csrf')) document.cookie = 'XSRF-TOKEN=fresh-csrf; Path=/';
      return Promise.resolve(new Response(null, { status: 204 }));
    });
    vi.stubGlobal('fetch', fetchMock);
    await apiFetch('/api/v1/auth/login', { method: 'POST' }, { refresh: false });
    expect(fetchMock.mock.calls.map(([url]) => new URL(url).pathname)).toEqual(['/api/v1/auth/csrf', '/api/v1/auth/login']);
    expect(fetchMock).toHaveBeenLastCalledWith(expect.any(String), expect.objectContaining({ headers: expect.objectContaining({ 'x-xsrf-token': 'fresh-csrf' }) }));
  });

  it('refreshes once and retries the original request including cookies', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(Response.json({})).mockResolvedValueOnce(Response.json({ value: 'restored' }));
    vi.stubGlobal('fetch', fetchMock);
    expect(await (await apiFetch('/api/v1/alerts')).json()).toEqual({ value: 'restored' });
    expect(fetchMock.mock.calls.map(([url]) => new URL(url).pathname)).toEqual(['/api/v1/alerts', '/api/v1/auth/refresh', '/api/v1/alerts']);
    expect(fetchMock.mock.calls.every(([, init]) => init.credentials === 'include')).toBe(true);
  });

  it('shares a refresh between simultaneous expired requests', async () => {
    let release!: (value: Response) => void;
    const refresh = new Promise<Response>((resolve) => { release = resolve; });
    let renewed = false;
    const fetchMock = vi.fn((url: string) => {
      if (url.endsWith('/refresh')) return refresh.then((value) => { renewed = true; return value; });
      return Promise.resolve(new Response(null, { status: renewed ? 204 : 401 }));
    });
    vi.stubGlobal('fetch', fetchMock);
    const first = apiFetch('/api/v1/alerts');
    const second = apiFetch('/api/v1/communications');
    await vi.waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1));
    release(new Response(null, { status: 204 }));
    await Promise.all([first, second]);
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1);
  });

  it('does not rotate again for a delayed 401 from before the completed refresh', async () => {
    let release!: (value: Response) => void;
    const delayed = new Promise<Response>((resolve) => { release = resolve; });
    let renewed = false;
    const fetchMock = vi.fn((url: string) => {
      if (url.endsWith('/refresh')) { renewed = true; return Promise.resolve(new Response(null, { status: 204 })); }
      if (url.endsWith('/slow') && !renewed) return delayed;
      return Promise.resolve(new Response(null, { status: renewed ? 204 : 401 }));
    });
    vi.stubGlobal('fetch', fetchMock);
    const slow = apiFetch('/api/v1/slow');
    await apiFetch('/api/v1/fast');
    release(new Response(null, { status: 401 }));
    await slow;
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1);
  });

  it('ends authentication after refresh rejection without retrying forever', async () => {
    const expired = vi.fn(); window.addEventListener(sessionExpiredEvent, expired);
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(apiFetch('/api/v1/alerts')).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(expired).toHaveBeenCalledOnce();
    window.removeEventListener(sessionExpiredEvent, expired);
  });

  it('retries protected requests at most once even when their renewed access is rejected', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 })).mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(apiFetch('/api/v1/alerts')).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('keeps authentication recoverable when the refresh network is unavailable', async () => {
    const expired = vi.fn(); window.addEventListener(sessionExpiredEvent, expired);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response(null, { status: 401 })).mockRejectedValueOnce(new TypeError('offline')));
    await expect(apiFetch('/api/v1/alerts')).rejects.toThrow('offline');
    expect(expired).not.toHaveBeenCalled();
    window.removeEventListener(sessionExpiredEvent, expired);
  });

  it('does not refresh login errors or CSRF denials and preserves structured problem codes', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(Response.json({ code: 'INSTITUTION_SELECTION_REQUIRED', institutions: [] }, { status: 409 }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(apiFetch('/api/v1/auth/login', { method: 'POST' }, { refresh: false })).rejects.toMatchObject({ status: 409, problem: { code: 'INSTITUTION_SELECTION_REQUIRED' } });
    await expect(apiFetch('/api/v1/alerts', { method: 'DELETE' })).rejects.toMatchObject({ status: 403 });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
