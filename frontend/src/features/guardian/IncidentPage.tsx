import { useEffect, useRef, useState } from 'react';
import * as L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { request } from '../../auth/api';
import type { Incident } from './SosDashboard';

function LocationMap({ point }: { point: NonNullable<Incident['location']> }) {
  const element = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!element.current) return;
    const map = L.map(element.current).setView([point.latitude, point.longitude], 15);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors', maxZoom: 19,
    }).addTo(map);
    L.circleMarker([point.latitude, point.longitude], { radius: 9, color: '#2563eb' }).addTo(map);
    return () => { map.remove(); };
  }, [point.latitude, point.longitude]);
  return <div className="g-map" ref={element} aria-label="SOS location map / অবস্থানের মানচিত্র" />;
}

export default function IncidentPage({ id }: { id: string }) {
  const [incident, setIncident] = useState<Incident | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    const refresh = () => void request<Incident>(`/guardian/incidents/${id}`).then(value => {
      if (active) { setIncident(value); setError(''); }
    }).catch(() => { if (active) { setIncident(null); setError('SOS unavailable, connection lost, or access changed. / SOS পাওয়া যাচ্ছে না অথবা অনুমতি বদলেছে।'); } });
    refresh(); const timer = setInterval(refresh, 3000);
    window.addEventListener('online', refresh); window.addEventListener('focus', refresh);
    return () => { active = false; clearInterval(timer); window.removeEventListener('online', refresh); window.removeEventListener('focus', refresh); };
  }, [id]);
  async function resolve() {
    const reason = window.prompt('Resolution note / সমাধানের বিবরণ');
    if (!reason?.trim()) return;
    setBusy(true); setError('');
    try {
      await request(`/guardian/incidents/${id}/resolve`, 'POST', { reason: reason.slice(0, 500) });
      setIncident(await request<Incident>(`/guardian/incidents/${id}`));
    } catch { setError('Could not resolve SOS / SOS সমাধান করা যায়নি'); }
    finally { setBusy(false); }
  }
  return <section>
    <a href="#/guardian">← SOS dashboard / ড্যাশবোর্ড</a><h1>SOS incident / SOS ঘটনা</h1>
    {error && <p role="alert">{error}</p>}
    {!incident && !error && <p role="status">Loading / লোড হচ্ছে…</p>}
    {incident && <article className="g-card">
      <h2>{incident.recipientName}</h2><p role="status">{incident.state}</p>
      <p>Opened / শুরু: {new Date(incident.openedAt).toLocaleString()}</p>
      <p>Location / অবস্থান: {incident.locationStatus}</p>
      {incident.location && <><p>Updated / আপডেট: {new Date(incident.location.observedAt).toLocaleString()} · ±{Math.round(incident.location.accuracy)} m</p><LocationMap point={incident.location} /></>}
      <p>Pickup / ঠিকানা: {incident.pickupAddress || 'Unavailable / পাওয়া যায়নি'}</p>
      {incident.canResolve && <button disabled={busy} onClick={() => void resolve()}>Mark resolved / সমাধান হয়েছে</button>}
    </article>}
  </section>;
}
