import { useEffect, useState } from 'react';
import { useRegisterSW } from 'virtual:pwa-register/react';
import { useLanguage } from './shared/i18n/useLanguage';

interface InstallEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: string }>;
}

export function PwaStatus({ suppressed = false }: { suppressed?: boolean }) {
  const { t } = useLanguage();

  const {
    needRefresh: [refresh, setRefresh],
    updateServiceWorker
  } = useRegisterSW();

  const [install, setInstall] = useState<InstallEvent | null>(null);

  useEffect(() => {
    const handler = (event: Event) => {
      event.preventDefault();
      setInstall(event as InstallEvent);
    };

    const installed = () => setInstall(null);

    window.addEventListener('beforeinstallprompt', handler);
    window.addEventListener('appinstalled', installed);

    return () => {
      window.removeEventListener('beforeinstallprompt', handler);
      window.removeEventListener('appinstalled', installed);
    };
  }, []);

  if (suppressed || (!refresh && !install)) return null;

  return (
    <aside
      className="pwa-status"
      aria-label={t(
        'App installation and updates',
        'অ্যাপ ইনস্টলেশন ও আপডেট'
      )}
    >
      <p>
        {refresh
          ? t(
              'An update is ready. Save your changes before reloading.',
              'আপডেট প্রস্তুত। আগে পরিবর্তন সংরক্ষণ করুন।'
            )
          : t(
              'Keep Pashe close. Install the app.',
              'পাশে অ্যাপটি ইনস্টল করুন।'
            )}
      </p>

      <button
        onClick={() => {
          if (refresh) {
            void updateServiceWorker(true);
          } else if (install) {
            void install.prompt()
              .then(() => install.userChoice)
              .then(() => setInstall(null));
          }
        }}
      >
        {refresh
          ? t('Update', 'আপডেট')
          : t('Install', 'ইনস্টল')}
      </button>

      <button
        onClick={() => {
          setRefresh(false);
          setInstall(null);
        }}
      >
        {t('Later', 'পরে')}
      </button>
    </aside>
  );
}
