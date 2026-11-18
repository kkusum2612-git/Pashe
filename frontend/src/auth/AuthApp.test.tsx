// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useEffect, useState } from 'react';
import AuthApp from './AuthApp';

function RouteHarness() {
  const [route, setRoute] = useState(location.hash.slice(7) || 'login');
  useEffect(() => {
    const update = () => setRoute(location.hash.slice(7));
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);
  return <AuthApp route={route} />;
}

const authenticatedUser = {
  id: 'user-1',
  fullName: 'Test User',
  email: 'tester@example.test',
  phone: '01712345678',
  emailVerified: true,
  roles: ['USER'],
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function installAuthApi(respondTo: (url: string, init?: RequestInit) => Response) {
  const fetchMock = vi.fn((url: string, init?: RequestInit) => Promise.resolve(respondTo(url, init)));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  location.hash = '';
});

describe('authentication screens', () => {
  it('signs in, opens the profile, and denies a role-only workspace', async () => {
    const user = userEvent.setup();
    const fetchMock = installAuthApi((url) => {
      if (url.endsWith('/auth/session')) return new Response(null, { status: 401 });
      if (url.endsWith('/auth/csrf')) return jsonResponse({ token: 'csrf-token' });
      if (url.endsWith('/auth/refresh')) return new Response(null, { status: 401 });
      if (url.endsWith('/auth/login')) return jsonResponse(authenticatedUser);
      throw new Error(`Unexpected API request: ${url}`);
    });

    location.hash = '#/auth/login';
    render(<RouteHarness />);
    await user.type(screen.getByLabelText(/Email/), 'tester@example.test');
    await user.type(screen.getByLabelText(/Password/), 'Correct-Password-2026!');
    await user.click(screen.getByRole('button', { name: /Sign in/ }));

    expect(await screen.findByText('Your profile')).toBeTruthy();
    expect(
      within(screen.getByRole('main')).getByText('tester@example.test')
    ).toBeTruthy();
    const loginCall = fetchMock.mock.calls.find(([url]) => url.endsWith('/auth/login'));
    expect(loginCall?.[1]?.method).toBe('POST');
    expect(JSON.parse(String(loginCall?.[1]?.body))).toEqual({
      email: 'tester@example.test',
      password: 'Correct-Password-2026!',
    });

    location.hash = '#/auth/admin';
    fireEvent(window, new Event('hashchange'));
    expect(await screen.findByRole('alert')).toBeTruthy();
    expect(screen.getByText(/do not have permission/i)).toBeTruthy();
  });

  it('submits registration and shows the email verification step', async () => {
    const user = userEvent.setup();
    const fetchMock = installAuthApi((url) => {
      if (url.endsWith('/auth/session')) return new Response(null, { status: 401 });
      if (url.endsWith('/auth/csrf')) return jsonResponse({ token: 'csrf-token' });
      if (url.endsWith('/auth/refresh')) return new Response(null, { status: 401 });
      if (url.endsWith('/auth/register')) return new Response(null, { status: 204 });
      throw new Error(`Unexpected API request: ${url}`);
    });

    location.hash = '#/auth/register';
    render(<RouteHarness />);
    await user.type(screen.getByLabelText(/Full name/), 'Test User');
    await user.type(screen.getByLabelText(/Email/), 'new@example.test');
    await user.type(screen.getByLabelText(/Phone number/), '01712345678');
    await user.type(screen.getByLabelText(/^Password/), 'Correct-Password-2026!');
    await user.type(screen.getByLabelText(/Confirm password/), 'Correct-Password-2026!');
    await user.click(screen.getByRole('button', { name: /Create account/ }));

    expect(await screen.findByRole('heading', { name: 'Verify your email' })).toBeTruthy();
    expect(await screen.findByText(/Check your inbox for a verification link/)).toBeTruthy();
    const registrationCall = fetchMock.mock.calls.find(([url]) => url.endsWith('/auth/register'));
    await waitFor(() => expect(registrationCall?.[1]?.method).toBe('POST'));
    expect(JSON.parse(String(registrationCall?.[1]?.body))).toEqual({
      fullName: 'Test User',
      email: 'new@example.test',
      phone: '01712345678',
      password: 'Correct-Password-2026!',
    });
  });
});
