import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, canAccess, request, validPhone } from './api';
import type { User } from './api';
afterEach(() => vi.unstubAllGlobals());
describe('authorization boundaries', () => {
  const user: User = { id: '1', fullName: 'Test', email: 'a@example.com', phone: '01712345678', emailVerified: true, roles: ['USER'] };
  it('denies anonymous, unverified, and incorrect roles', () => {
    expect(canAccess(null, ['ADMIN'])).toBe(false);
    expect(canAccess(user, ['ADMIN'])).toBe(false);
    expect(canAccess({ ...user, emailVerified: false }, ['USER'])).toBe(false);
    expect(canAccess(user, ['USER'])).toBe(true);
  });
  it('validates local and international Bangladesh numbers', () => {
    expect(validPhone('01712345678')).toBe(true);
    expect(validPhone('+8801712345678')).toBe(true);
    expect(validPhone('01212345678')).toBe(false);
    expect(validPhone('abc')).toBe(false);
  });
});
describe('session transport', () => {
  it('refreshes an expired access cookie and retries the session request', async () => {
    const json = (body: unknown) => new Response(JSON.stringify(body), { headers: { 'Content-Type': 'application/json' } });
    const fetch = vi.fn().mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(json({ token: 'csrf' })).mockResolvedValueOnce(json({ id: '1' })).mockResolvedValueOnce(json({ id: '1' }));
    vi.stubGlobal('fetch', fetch);
    await expect(request('/auth/session')).resolves.toEqual({ id: '1' });
    expect(fetch.mock.calls.map(call => call[0])).toEqual(['/api/v1/auth/session', '/api/v1/auth/csrf', '/api/v1/auth/refresh', '/api/v1/auth/session']);
  });
  it('gets CSRF token before a mutation and includes session cookies', async () => {
    const fetch = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf-test' }), { headers: { 'Content-Type': 'application/json' } })).mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    await request('/auth/logout', 'POST');
    expect(fetch.mock.calls[0][0]).toBe('/api/v1/auth/csrf');
    expect(fetch.mock.calls[1][1]).toMatchObject({ credentials: 'include', cache: 'no-store', headers: { 'X-CSRF-TOKEN': 'csrf-test' } });
  });
  it('does not send the mutation if CSRF retrieval fails', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(null, { status: 503 }));
    vi.stubGlobal('fetch', fetch);
    await expect(request('/auth/login', 'POST', {})).rejects.toBeInstanceOf(ApiError);
    expect(fetch).toHaveBeenCalledTimes(1);
  });
  it('rejects HTML fallbacks instead of treating them as sessions', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html />', { headers: { 'Content-Type': 'text/html' } })));
    await expect(request('/auth/session')).rejects.toBeInstanceOf(ApiError);
  });
  it('notifies the UI when a protected request loses its session', async () => {
    const dispatchEvent = vi.fn();
    vi.stubGlobal('window', { dispatchEvent });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    await expect(request('/users/me', 'GET', undefined, false)).rejects.toBeInstanceOf(ApiError);
    expect(dispatchEvent.mock.calls[0][0].type).toBe('pashe:session-expired');
  });
  it('does not treat an anonymous session check as an expiry notification', async () => {
    const dispatchEvent = vi.fn();
    vi.stubGlobal('window', { dispatchEvent });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    await expect(request('/auth/session', 'GET', undefined, false)).rejects.toBeInstanceOf(ApiError);
    expect(dispatchEvent).not.toHaveBeenCalled();
  });
});
