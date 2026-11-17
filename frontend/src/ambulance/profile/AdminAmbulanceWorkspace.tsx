import { useEffect, useRef, useState } from 'react';
import { ambulanceApi } from './api';
import type { AmbulanceProfile } from './types';
import './ambulance.css';

export default function AdminAmbulanceWorkspace({
  offline = false
}: {
  offline?: boolean
}) {
  const [profiles, setProfiles] = useState<AmbulanceProfile[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [notes, setNotes] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const pending = useRef(false);

  const selected =
    profiles.find(item => item.id === selectedId) ?? profiles[0];

  useEffect(() => {
    let active = true;

    ambulanceApi.getPendingAmbulances()
      .then(items => {
        if (active) setProfiles(items);
      })
      .catch(() => {
        if (active) setError('Unable to load pending ambulances.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  async function perform(
    task: () => Promise<void>,
    message = ''
  ) {
    if (pending.current || offline) return;

    pending.current = true;
    setBusy(true);
    setError('');
    setNotice('');

    try {
      await task();
      setNotice(message);
    } catch {
      setError('Request failed. Refresh the application and try again.');
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  function review(status: 'APPROVED' | 'REJECTED') {
    if (!selected) return;

    if (status === 'REJECTED' && !notes.trim()) {
      setError('Provide a reason for rejection.');
      return;
    }

    void perform(async () => {
      await ambulanceApi.review(selected.id, status, notes.trim());

      setProfiles(items =>
        items.filter(item => item.id !== selected.id)
      );

      setSelectedId('');
      setConfirmed(false);
      setNotes('');
    }, `Ambulance ${status.toLowerCase()} successfully.`);
  }

  if (loading) {
    return <p role="status">Loading pending applications...</p>;
  }

  return (
    <div className="am-workspace" aria-busy={busy}>
      <div className="am-heading">
        <div>
          <h3>Ambulance verification</h3>
          <p>Review vehicle registrations and submitted licenses.</p>
        </div>

        <button
          className="am-secondary"
          disabled={busy || offline}
          onClick={() => void perform(async () => {
            setProfiles(await ambulanceApi.getPendingAmbulances());
            setConfirmed(false);
            setNotes('');
          })}
        >
          Refresh
        </button>
      </div>

      {error && <p className="auth-alert" role="alert">{error}</p>}
      {notice && <p className="auth-success" role="status">{notice}</p>}

      <div className="am-layout">
        <aside className="am-panel">
          <h3>Pending applications ({profiles.length})</h3>

          {profiles.length === 0 && (
            <p className="am-muted">No pending applications.</p>
          )}

          <div className="am-vehicle-list">
            {profiles.map(item => (
              <button
                key={item.id}
                className={
                  'am-vehicle-item' +
                  (selected?.id === item.id ? ' am-selected' : '')
                }
                disabled={busy}
                onClick={() => {
                  setSelectedId(item.id);
                  setNotes('');
                  setConfirmed(false);
                }}
              >
                <strong>{item.vehicleName}</strong>
                <span>{item.registrationNumber}</span>
                <span className="am-status am-pending">PENDING</span>
              </button>
            ))}
          </div>
        </aside>

        {selected && (
          <div className="am-details">
            <section className="am-panel">
              <h3>{selected.vehicleName}</h3>

              <p>Registration: {selected.registrationNumber}</p>
              <p>Vehicle type: {selected.vehicleType}</p>
            </section>

            <section className="am-panel">
              <h3>Declared equipment</h3>

              {selected.equipment.length === 0 && (
                <p className="am-muted">No equipment submitted.</p>
              )}

              {selected.equipment.map(item => (
                <div className="am-list-row" key={item.id}>
                  <span>{item.equipmentName}</span>
                  <strong>Quantity: {item.quantity}</strong>
                </div>
              ))}
            </section>

            <section className="am-panel">
              <h3>License documents</h3>

              {selected.documents.length === 0 && (
                <p className="am-muted">No documents submitted.</p>
              )}

              {selected.documents.map(doc => (
                <div className="am-list-row" key={doc.id}>
                  <span>{doc.fileName}</span>

                  <button
                    className="am-secondary"
                    disabled={busy || offline}
                    onClick={() => void perform(() =>
                      ambulanceApi.downloadDocument(
                        selected.id,
                        doc.id,
                        doc.fileName,
                        true
                      )
                    )}
                  >
                    Download
                  </button>
                </div>
              ))}
            </section>

            <section className="am-panel">
              <h3>Verification decision</h3>

              <label>
                Review notes
                <textarea
                  value={notes}
                  maxLength={1000}
                  rows={4}
                  onChange={event => setNotes(event.target.value)}
                />
              </label>

              <label className="am-check">
                <input
                  type="checkbox"
                  checked={confirmed}
                  onChange={event => setConfirmed(event.target.checked)}
                />
                I have reviewed the documents and equipment.
              </label>

              <div className="am-actions">
                <button
                  className="am-primary"
                  disabled={
                    busy ||
                    offline ||
                    !confirmed ||
                    selected.documents.length === 0 ||
                    selected.equipment.length === 0
                  }
                  onClick={() => review('APPROVED')}
                >
                  Approve ambulance
                </button>

                <button
                  className="am-danger"
                  disabled={busy || offline}
                  onClick={() => review('REJECTED')}
                >
                  Reject application
                </button>
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}