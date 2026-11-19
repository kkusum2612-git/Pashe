import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { request } from '../../auth/api';
export interface Membership { id: string; careRecipientId: string; inviteeId: string; recipientName: string; inviteeName: string; relationshipType: string; status: string }
export default function CareCircle({ userId }: { userId: string }) {
  const [items, setItems] = useState<Membership[]>([]);
  const [incoming, setIncoming] = useState(false);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const load = () => request<Membership[]>(`/guardian/care-circle?page=${page}`).then(setItems);
  useEffect(() => { let active = true; request<Membership[]>(`/guardian/care-circle?page=${page}`).then(value => { if (active) setItems(value); }).catch(() => { if (active) setError('Unable to load Care Circle / কেয়ার সার্কেল লোড হয়নি'); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [page]);
  async function run(task: () => Promise<unknown>) { setBusy(true); setError(''); setMessage(''); try { await task(); await load(); } catch { setError('Could not save. Refresh and check your permissions. / সংরক্ষণ হয়নি। আবার চেষ্টা করুন।'); } finally { setBusy(false); } }
  function invite(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = event.currentTarget; const data = new FormData(form); void run(async () => { await request('/guardian/care-circle/invitations', 'POST', { email: data.get('email'), relationshipType: data.get('type'), consent: true }); form.reset(); setMessage('If the account is eligible, an invitation is available in their Care Circle. / যোগ্য অ্যাকাউন্টে আমন্ত্রণ পাঠানো হয়েছে।'); }); }
  const act = (id: string, action: string) => run(() => request(`/guardian/care-circle/memberships/${id}/${action}`, 'POST'));
  return <section><h1>Care Circle <small lang="bn">কেয়ার সার্কেল</small></h1><p>Both people must agree before care access is granted. / উভয়ের সম্মতি প্রয়োজন।</p>
    {error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
    <form onSubmit={invite}><fieldset disabled={busy}><label>Email / ইমেইল<input name="email" type="email" required maxLength={254} /></label><label>Relationship / সম্পর্ক<select name="type"><option value="GUARDIAN">Guardian / অভিভাবক</option><option value="TRUSTED_NEIGHBOR">Trusted neighbor / বিশ্বস্ত প্রতিবেশী</option></select></label><label className="g-check"><input type="checkbox" required />I consent to invite this person / আমন্ত্রণে সম্মতি দিচ্ছি</label><button>Send invitation / আমন্ত্রণ পাঠান</button></fieldset></form>
    <div className="g-actions"><button aria-pressed={!incoming} onClick={() => setIncoming(false)}>My circle / আমার সার্কেল</button><button aria-pressed={incoming} onClick={() => setIncoming(true)}>Incoming / প্রাপ্ত আমন্ত্রণ</button><button disabled={busy} onClick={() => void run(load)}>Refresh / রিফ্রেশ</button></div>
    {loading ? <p role="status">Loading / লোড হচ্ছে…</p> : !items.filter(m => incoming ? m.inviteeId === userId : m.careRecipientId === userId).length && <p>No memberships on this page / এই পাতায় সদস্য নেই</p>}
    {items.filter(m => incoming ? m.inviteeId === userId : m.careRecipientId === userId).map(m => <article className="g-card" key={m.id}><h3>{incoming ? m.recipientName : m.inviteeName}</h3><p>{m.relationshipType} · {m.status}</p><div className="g-actions">
      {m.status === 'PENDING' && (incoming ? <><button disabled={busy} onClick={() => void act(m.id, 'accept')}>Accept / গ্রহণ</button><button disabled={busy} onClick={() => void act(m.id, 'decline')}>Decline / প্রত্যাখ্যান</button></> : <button disabled={busy} onClick={() => void act(m.id, 'cancel')}>Cancel invitation / বাতিল</button>)}
      {m.status === 'ACTIVE' && <><button disabled={busy} onClick={() => { if (window.confirm('Remove this care relationship? / সম্পর্কটি বাতিল করবেন?')) void act(m.id, 'revoke'); }}>Remove / সম্পর্ক বাতিল</button></>}
    </div></article>)}
    <div className="g-actions"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous / পূর্ববর্তী</button><span>Page {page + 1}</span><button disabled={items.length < 50} onClick={() => setPage(page + 1)}>Next / পরবর্তী</button></div>
  </section>;
}
