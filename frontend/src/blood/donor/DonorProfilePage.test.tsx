// @vitest-environment jsdom

import {
  cleanup,
  fireEvent,
  render,
  screen,
} from '@testing-library/react';

import userEvent from '@testing-library/user-event';

import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import DonorProfilePage from './DonorProfilePage';
import type { DonorProfile } from './api';

const existingProfile: DonorProfile = {
  userId: 'test-user',
  bloodGroup: 'B+',
  area: 'Mirpur, Dhaka',
  lastDonationDate: '2020-01-15',
  available: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json',
    },
  });
}

function installApi(
  respondTo: (url: string, init?: RequestInit) => Response
) {
  const fetchMock = vi.fn(
    (url: string, init?: RequestInit) =>
      Promise.resolve(respondTo(url, init))
  );

  vi.stubGlobal('fetch', fetchMock);

  return fetchMock;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('donor profile', () => {

  it('loads and displays an existing profile', async () => {
    installApi(() => jsonResponse(existingProfile));

    render(<DonorProfilePage />);

    const group = await screen.findByRole('combobox', {
      name: /Blood group/i,
    }) as HTMLSelectElement;

    expect(group.value).toBe('B+');

    expect(
      (screen.getByLabelText('Area') as HTMLInputElement).value
    ).toBe('Mirpur, Dhaka');

    expect(
      (screen.getByLabelText('Last donation date') as HTMLInputElement).value
    ).toBe('2020-01-15');

    expect(
      (screen.getByRole('checkbox') as HTMLInputElement).checked
    ).toBe(true);
  });

  it('creates a new donor profile', async () => {
    const user = userEvent.setup();

    const fetchMock = installApi((url, init) => {
      if (url.endsWith('/auth/csrf')) {
        return jsonResponse({ token: 'csrf-token' });
      }

      if (url.endsWith('/blood/donor-profile')) {
        if (init?.method === 'PUT') {
          return jsonResponse({
            ...existingProfile,
            ...JSON.parse(String(init.body)),
          });
        }

        return new Response(null, { status: 404 });
      }

      throw new Error(`Unexpected API request: ${url}`);
    });

    render(<DonorProfilePage />);

    await user.selectOptions(
      await screen.findByRole('combobox'),
      'O+'
    );

    await user.type(
      screen.getByLabelText('Area'),
      'Uttara, Dhaka'
    );

    await user.click(screen.getByRole('checkbox'));

    await user.click(
      screen.getByRole('button', {
        name: /Save donor profile/i,
      })
    );

    expect(
      await screen.findByText('Donor profile saved successfully.')
    ).toBeTruthy();

    const saveCall = fetchMock.mock.calls.find(
      ([url, init]) =>
        url.endsWith('/blood/donor-profile') &&
        init?.method === 'PUT'
    );

    expect(saveCall).toBeDefined();

    expect(
      JSON.parse(String(saveCall?.[1]?.body))
    ).toEqual({
      bloodGroup: 'O+',
      area: 'Uttara, Dhaka',
      lastDonationDate: null,
      available: true,
    });
  });

  it('updates the donation date and availability', async () => {
    const user = userEvent.setup();

    const fetchMock = installApi((url, init) => {
      if (url.endsWith('/auth/csrf')) {
        return jsonResponse({ token: 'csrf-token' });
      }

      if (init?.method === 'PUT') {
        return jsonResponse({
          ...existingProfile,
          ...JSON.parse(String(init.body)),
        });
      }

      return jsonResponse(existingProfile);
    });

    render(<DonorProfilePage />);

    fireEvent.change(
      await screen.findByLabelText('Last donation date'),
      { target: { value: '2021-02-15' } }
    );

    await user.click(screen.getByRole('checkbox'));

    await user.click(
      screen.getByRole('button', {
        name: /Save donor profile/i,
      })
    );

    expect(
      await screen.findByText('Donor profile saved successfully.')
    ).toBeTruthy();

    const saveCall = fetchMock.mock.calls.find(
      ([url, init]) =>
        url.endsWith('/blood/donor-profile') &&
        init?.method === 'PUT'
    );

    expect(
      JSON.parse(String(saveCall?.[1]?.body))
    ).toEqual({
      bloodGroup: 'B+',
      area: 'Mirpur, Dhaka',
      lastDonationDate: '2021-02-15',
      available: false,
    });
  });

  it('displays an error when loading fails', async () => {
    installApi(() => new Response(null, { status: 500 }));

    render(<DonorProfilePage />);

    const alert = await screen.findByRole('alert');

    expect(alert.textContent).toContain(
      'Unable to load donor profile.'
    );

    expect(
      screen.getByRole('button', { name: 'Try again' })
    ).toBeTruthy();
  });

  it('displays an error when saving fails', async () => {
    const user = userEvent.setup();

    installApi((url, init) => {
      if (url.endsWith('/auth/csrf')) {
        return jsonResponse({ token: 'csrf-token' });
      }

      if (init?.method === 'PUT') {
        return new Response(null, { status: 500 });
      }

      return jsonResponse(existingProfile);
    });

    render(<DonorProfilePage />);

    await user.click(
      await screen.findByRole('button', {
        name: /Save donor profile/i,
      })
    );

    expect(
      (await screen.findByRole('alert')).textContent
    ).toContain('Unable to save your profile.');
  });

  it('prevents submission while offline', async () => {
    installApi(() => jsonResponse(existingProfile));

    render(<DonorProfilePage offline />);

    const button = await screen.findByRole('button', {
      name: /Save donor profile/i,
    }) as HTMLButtonElement;

    expect(button.disabled).toBe(true);
  });
});