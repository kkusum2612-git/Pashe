import { useEffect, useState } from 'react';
import { useRegisterSW } from 'virtual:pwa-register/react';

interface InstallEvent extends Event { prompt(): Promise<void>; userChoice: Promise<{ outcome: string }> }
export function PwaStatus() {
  const { needRefresh: [refresh, setRefresh], updateServiceWorker } = useRegisterSW();
  const [install, setInstall] = useState<InstallEvent | null>(null);
  useEffect(() => {
    const handler = (event: Event) => { event.preventDefault(); setInstall(event as InstallEvent); };
    const installed = () => setInstall(null);
    window.addEventListener('beforeinstallprompt', handler);
    window.addEventListener('appinstalled', installed);
    return () => { window.removeEventListener('beforeinstallprompt', handler); window.removeEventListener('appinstalled', installed); };
  }, []);
  if (!refresh && !install) return null;
  return <aside className="pwa-status" aria-label="App installation and updates">
    <p>{refresh ? 'An update is ready. Save your changes before reloading. / আপডেট প্রস্তুত। আগে পরিবর্তন সংরক্ষণ করুন।' : 'Keep Pashe close. Install the app. / পাশে অ্যাপটি ইনস্টল করুন।'}</p>
    <button onClick={() => { if (refresh) void updateServiceWorker(true); else if (install) void install.prompt().then(() => install.userChoice).then(() => setInstall(null)); }}>{refresh ? 'Update / আপডেট' : 'Install / ইনস্টল'}</button>
    <button onClick={() => { setRefresh(false); setInstall(null); }}>Later / পরে</button>
  </aside>;
}
