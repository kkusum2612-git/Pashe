// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ComponentProps } from 'react';
import type { User } from '../../auth/api';
import AppLayout from './AppLayout';
import { getNavigation } from './navigation';

const user: User = {
  id: 'user-1',
  fullName: 'Test User',
  email: 'tester@example.test',
  phone: '01712345678',
  emailVerified: true,
  roles: ['USER'],
};

function renderLayout(overrides: Partial<ComponentProps<typeof AppLayout>> = {}) {
  const props = {
    user,
    activePage: 'dashboard',
    bn: false,
    onToggleLanguage: vi.fn(),
    onSignOut: vi.fn(),
    children: <p>Page content</p>,
    ...overrides,
  };

  const view = render(<AppLayout {...props} />);

  const sidebar = view.container.querySelector<HTMLElement>('.app-sidebar')!;
  const drawer = view.container.querySelector<HTMLDialogElement>('.app-drawer')!;

  // Simulate native dialog behavior in the test environment.
  Object.defineProperty(drawer, 'showModal', {
    configurable: true,
    value: vi.fn(() => {
      drawer.setAttribute('open', '');
    }),
  });

  Object.defineProperty(drawer, 'close', {
    configurable: true,
    value: vi.fn(() => {
      drawer.removeAttribute('open');
      drawer.dispatchEvent(new Event('close'));
    }),
  });

  return { ...view, props, sidebar, drawer };
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  location.hash = '';
});

describe('shared application layout', () => {
  it('shows the appropriate navigation and highlights the current page', () => {
    const { sidebar } = renderLayout();

    expect(within(sidebar).getByText('Dashboard')).toBeTruthy();
    expect(within(sidebar).getByText('Blood Network')).toBeTruthy();
    expect(within(sidebar).getByText('My Profile')).toBeTruthy();

    expect(within(sidebar).queryByText('Administration')).toBeNull();

    expect(
      within(sidebar).getByText('Dashboard')
        .closest('a')?.getAttribute('aria-current')
    ).toBe('page');

    expect(screen.getByText('Page content')).toBeTruthy();
  });

  it('filters navigation according to account roles', () => {
    const adminUser: User = {
      ...user,
      roles: ['USER', 'ADMIN'],
    };

    const pages = getNavigation(adminUser).map(item => item.page);

    expect(pages).toContain('admin');
    expect(pages).not.toContain('ambulance');
    expect(pages).not.toContain('pharmacy');

    expect(
      getNavigation({ ...user, emailVerified: false })
    ).toEqual([]);
  });

  it('opens the mobile drawer and closes it after navigation', () => {
    const { drawer } = renderLayout();

    const menuButton = screen.getByLabelText('Open navigation');

    expect(drawer.open).toBe(false);

    fireEvent.click(menuButton);

    expect(drawer.open).toBe(true);
    expect(menuButton.getAttribute('aria-expanded')).toBe('true');

    fireEvent.click(within(drawer).getByText('Blood Network'));

    expect(drawer.open).toBe(false);
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
  });

  it('closes the drawer using its close button or backdrop', () => {
    const { drawer } = renderLayout();

    fireEvent.click(screen.getByLabelText('Open navigation'));

    fireEvent.click(screen.getByLabelText('Close navigation'));

    expect(drawer.open).toBe(false);

    fireEvent.click(screen.getByLabelText('Open navigation'));

    fireEvent.click(drawer);

    expect(drawer.open).toBe(false);
  });

  it('calls sign out and disables it when offline', () => {
    const { sidebar, props, rerender } = renderLayout();

    const button = within(sidebar).getByText('Sign out') as HTMLButtonElement;

    fireEvent.click(button);

    expect(props.onSignOut).toHaveBeenCalledTimes(1);

    rerender(<AppLayout {...props} offline />);

    expect(button.disabled).toBe(true);
  });
});