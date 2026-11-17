export type Role = 'USER' | 'ADMIN' | 'AMBULANCE_OPERATOR' | 'PHARMACY_STAFF';
export interface User { id: string; fullName: string; email: string; phone: string; emailVerified: boolean; roles: Role[] }
export class ApiError extends Error {
  status: number;
  constructor(status: number) { super(`Request failed (${status})`); this.status = status; }
}

// Session cookies are managed exclusively by the browser/server, never localStorage.
let refreshing: Promise<unknown> | null = null;
export async function request<T>(path: string, method = 'GET', body?: unknown, retry = true): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (method !== 'GET') {
    const csrf = await request<{ token: string }>('/auth/csrf');
    headers['X-CSRF-TOKEN'] = csrf.token;
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(`/api/v1${path}`, {
    method, credentials: 'include', cache: 'no-store', headers, signal: AbortSignal.timeout(15000),
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    if (response.status === 401 && retry && (path === '/auth/session' || !path.startsWith('/auth/'))) {
      try {
        refreshing ??= request('/auth/refresh', 'POST', undefined, false).finally(() => { refreshing = null; });
        await refreshing;
      } catch (error) {
        if (path !== '/auth/session' && error instanceof ApiError && error.status === 401) window.dispatchEvent(new Event('pashe:session-expired'));
        throw error;
      }
      return request<T>(path, method, body, false);
    }
    if (response.status === 401 && !['/auth/login', '/auth/session', '/auth/refresh'].includes(path)) {
      window.dispatchEvent(new Event('pashe:session-expired'));
    }
    throw new ApiError(response.status);
  }
  if (response.status === 204) return undefined as T;
  if (!response.headers.get('content-type')?.includes('application/json')) throw new ApiError(503);
  return response.json() as Promise<T>;
}

export function canAccess(user: User | null, roles: Role[]) {
  return !!user?.emailVerified && roles.some(role => user.roles.includes(role));
}
export function validPhone(value: string) { return /^(?:\+8801|01)[3-9]\d{8}$/.test(value.replace(/[\s-]/g, '')); }
