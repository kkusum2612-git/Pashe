// @vitest-environment jsdom

import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { useLanguage } from './useLanguage';

beforeEach(() => {
  localStorage.removeItem('pashe-language');
});

afterEach(() => {
  cleanup();
  localStorage.removeItem('pashe-language');
  document.documentElement.lang = 'en';
  document.title = 'Pashe';
});

describe('shared language preference', () => {
  it('uses English by default', () => {
    const { result } = renderHook(() => useLanguage());

    expect(result.current.language).toBe('en');
    expect(result.current.bn).toBe(false);

    expect(
      result.current.t('Dashboard', 'ড্যাশবোর্ড')
    ).toBe('Dashboard');
  });

  it('switches languages and persists the preference', () => {
    const { result, unmount } = renderHook(() => useLanguage());

    act(() => {
      result.current.toggleLanguage();
    });

    expect(result.current.language).toBe('bn');
    expect(localStorage.getItem('pashe-language')).toBe('bn');
    expect(document.documentElement.lang).toBe('bn');
    expect(document.title).toBe('পাশে');

    unmount();

    const next = renderHook(() => useLanguage());

    expect(next.result.current.language).toBe('bn');
    expect(
      next.result.current.t('Dashboard', 'ড্যাশবোর্ড')
    ).toBe('ড্যাশবোর্ড');
  });

  it('synchronizes language preferences between components', () => {
    const first = renderHook(() => useLanguage());
    const second = renderHook(() => useLanguage());

    act(() => {
      first.result.current.setLanguage('bn');
    });

    expect(second.result.current.language).toBe('bn');

    act(() => {
      second.result.current.setLanguage('en');
    });

    expect(first.result.current.language).toBe('en');
  });
});