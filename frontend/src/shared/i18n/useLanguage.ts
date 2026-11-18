import { useEffect, useSyncExternalStore } from 'react';

export type Language = 'en' | 'bn';
export type LocalizedText = [string, string];

const storageKey = 'pashe-language';
const changeEvent = 'pashe:language-changed';

let fallbackLanguage: Language = 'en';

function getLanguage(): Language {
  try {
    return localStorage.getItem(storageKey) === 'bn' ? 'bn' : 'en';
  } catch {
    return fallbackLanguage;
  }
}

function subscribe(listener: () => void) {
  window.addEventListener(changeEvent, listener);
  window.addEventListener('storage', listener);

  return () => {
    window.removeEventListener(changeEvent, listener);
    window.removeEventListener('storage', listener);
  };
}

function setLanguage(language: Language) {
  fallbackLanguage = language;

  try {
    localStorage.setItem(storageKey, language);
  } catch {
    // Continue using the in-memory preference.
  }

  window.dispatchEvent(new Event(changeEvent));
}

export function useLanguage() {
  const language = useSyncExternalStore(
    subscribe,
    getLanguage,
    () => 'en'
  );

  const bn = language === 'bn';

useEffect(() => {
  document.documentElement.lang = language;
  document.title = language === 'bn' ? 'পাশে' : 'Pashe';
}, [language]);

  function toggleLanguage() {
    setLanguage(bn ? 'en' : 'bn');
  }

  function t(en: string, bangla: string) {
    return bn ? bangla : en;
  }

  return {
    language,
    bn,
    t,
    setLanguage,
    toggleLanguage,
  };
}