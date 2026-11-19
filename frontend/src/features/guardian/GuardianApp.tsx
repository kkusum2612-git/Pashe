import { useEffect, useState } from 'react';
import { request } from '../../auth/api';
import type { User } from '../../auth/api';
import CareCircle from './CareCircle';
import SosDashboard from './SosDashboard';
import IncidentPage from './IncidentPage';
import LocationSharing from './LocationSharing';
import './guardian.css';

export default function GuardianApp({ route = '' }: { route?: string }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    void request<User>('/auth/session').then(value => { if (active) setUser(value); })
      .catch(() => {}).finally(() => { if (active) setLoading(false); });
    const expire = () => setUser(null);
    window.addEventListener('pashe:session-expired', expire);
    return () => { active = false; window.removeEventListener('pashe:session-expired', expire); };
  }, []);
  return <main className="guardian">
    <header><a href="#/auth/dashboard">✚ Pashe / পাশে</a><a href="#/auth/profile">Account / অ্যাকাউন্ট</a></header>
    {loading ? <p role="status">Checking account / অ্যাকাউন্ট যাচাই হচ্ছে…</p> : user?.emailVerified ? <>
      <nav className="g-actions"><a href="#/guardian">SOS</a><a href="#/guardian/care-circle">Care Circle / কেয়ার সার্কেল</a></nav>
      {route.startsWith('/incidents/') ? <IncidentPage key={route} id={route.slice(11)} />
        : route === '/care-circle' ? <CareCircle userId={user.id} />
          : route === '' ? <SosDashboard /> : <p>Page unavailable / পাতা পাওয়া যায়নি</p>}
      <LocationSharing />
    </> : <p><a href="#/auth/login">Sign in with a verified account / যাচাইকৃত অ্যাকাউন্টে প্রবেশ করুন</a></p>}
  </main>;
}
