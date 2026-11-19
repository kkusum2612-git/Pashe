
// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it, vi } from 'vitest';
import MedicineCarePage from './MedicineCarePage';

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

it('requires confirmation and saves the previewed dosage, then reloads the saved record', async () => {
  const user = userEvent.setup();
  let medicines: unknown[] = [];
  const mockFetch = vi.fn(async (path: string, options?: RequestInit) => {
    const method = options?.method ?? 'GET';
    if (path.endsWith('/auth/csrf')) return json({ token: 'synthetic' });
    if (path.endsWith('/my-medicines/preview')) {
      const payload = JSON.parse(String(options?.body));
      return json({
        ...payload, medicineName: 'Synthetic custom', normalizedPattern: '0.5+0+1',
        doses: [{ slot: 'MORNING', quantity: 0.5, meal: 'breakfast', scheduledAt: '2026-11-19T08:00:00+06:00' }],
        safety: { allergyCheckAvailable: false, warnings: [] },
      });
    }
    if (path.endsWith('/my-medicines')) {
      if (method === 'POST') {
        const payload = JSON.parse(String(options?.body));
        const medicine = {
          ...payload, id: 'm1', medicineName: 'Synthetic custom', status: 'ACTIVE',
          customMedicine: true, dosePattern: '0.5+0+1',
        };
        medicines = [medicine];
        return json(medicine, 201);
      }
      return json(medicines);
    }
    if (path.endsWith('/preferences')) {
      return json({
        breakfastTime: '08:00', lunchTime: '13:00', dinnerTime: '20:00', bedtime: '22:30',
        timezone: 'Asia/Dhaka', ramadan: null,
      });
    }
    if (path.endsWith('/safety')) return json({ allergyCheckAvailable: false, warnings: [] });
    return json([]);
  });
  vi.stubGlobal('fetch', mockFetch);
  render(<MedicineCarePage offline={false} />);

  await user.click(await screen.findByRole('checkbox', { name: 'Add a custom medicine' }));
  await user.type(screen.getAllByRole('textbox', { name: 'Medicine name' })[1], 'Synthetic custom');
  await user.type(screen.getByRole('textbox', { name: 'Dose unit' }), 'tablet');
  await user.click(screen.getByText('Pick doses by time of day'));
  await user.click(screen.getByRole('button', { name: 'Morning 0.5' }));
  await user.click(screen.getByRole('button', { name: 'Night 1' }));
  await user.click(screen.getByRole('button', { name: 'Preview medicine' }));

  const confirm = await screen.findByRole('button', { name: 'Confirm and save' });
  const saves = () => mockFetch.mock.calls.filter(
    ([path, init]) => path.endsWith('/my-medicines') && init?.method === 'POST'
  );
  expect(saves()).toHaveLength(0);
  expect(screen.getByText(/0.5 tablet at breakfast/)).toBeTruthy();
  await user.click(screen.getByRole('button', { name: 'Back to details' }));
  expect(saves()).toHaveLength(0);
  expect(screen.queryByRole('button', { name: 'Confirm and save' })).toBeNull();
  await user.click(screen.getByRole('button', { name: 'Preview medicine' }));
  const nextConfirm = await screen.findByRole('button', { name: 'Confirm and save' });
  expect(nextConfirm).not.toBe(confirm);
  await user.click(nextConfirm);
  expect(await screen.findByRole('heading', { name: 'Synthetic custom' })).toBeTruthy();
  expect(saves()).toHaveLength(1);
  expect(JSON.parse(String(saves()[0][1]?.body)).dosePattern).toBe('0.5+0+1');
});
