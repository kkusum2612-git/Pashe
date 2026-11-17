import { useEffect, useState } from 'react';
import { ambulanceApi } from './api';
import type { VerifiedAmbulance } from './types';
import './ambulance.css';

export default function VerifiedAmbulanceDirectory({
  offline = false
}: {
  offline?: boolean
}) {
  const [ambulances, setAmbulances] =
    useState<VerifiedAmbulance[]>([]);

  const [search, setSearch] = useState('');
  const [availableOnly, setAvailableOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    ambulanceApi.getVerifiedAmbulances()
      .then(items => {
        if (active) setAmbulances(items);
      })
      .catch(() => {
        if (active) setError('Unable to load verified ambulances.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  async function refresh() {
    setLoading(true);
    setError('');

    try {
      setAmbulances(await ambulanceApi.getVerifiedAmbulances());
    } catch {
      setError('Unable to refresh ambulance information.');
    } finally {
      setLoading(false);
    }
  }

  const query = search.trim().toLowerCase();

  const filtered = ambulances.filter(item => {
    const text = [
      item.vehicleName,
      item.registrationNumber,
      item.vehicleType,
      ...item.equipment.map(eq => eq.equipmentName)
    ].join(' ').toLowerCase();

    return text.includes(query) &&
      (!availableOnly || item.available);
  });

  return (
    <div className="am-workspace">
      <div className="am-heading">
        <div>
          <h3>Verified ambulance directory</h3>
          <p>Explore approved vehicles and their medical equipment.</p>
        </div>

        <button
          className="am-secondary"
          disabled={loading || offline}
          onClick={() => void refresh()}
        >
          Refresh
        </button>
      </div>

      <section className="am-panel">
        <label>
          Search ambulances
          <input
            value={search}
            placeholder="Vehicle name, type or equipment"
            onChange={event => setSearch(event.target.value)}
          />
        </label>

        <label className="am-check">
          <input
            type="checkbox"
            checked={availableOnly}
            onChange={event => setAvailableOnly(event.target.checked)}
          />
          Show available ambulances only
        </label>
      </section>

      {error && <p className="auth-alert" role="alert">{error}</p>}

      {loading ? (
        <p role="status">Loading ambulances...</p>
      ) : (
        <>
          <p className="am-muted">{filtered.length} ambulances found</p>

          <div className="am-directory-grid">
            {filtered.map(item => (
              <article className="am-panel" key={item.id}>

                <div className="am-heading">
                  <h3>{item.vehicleName}</h3>
                  <span className="am-status am-approved">Verified</span>
                </div>

                <p>{item.registrationNumber}</p>
                <p>Type: {item.vehicleType}</p>

                <span className={
                  `am-status ${item.available ? 'am-approved' : 'am-pending'}`
                }>
                  {item.available ? 'Available' : 'Unavailable'}
                </span>

                <h4>Verified equipment</h4>

                {item.equipment.map(eq => (
                  <div className="am-list-row" key={eq.id}>
                    <span>{eq.equipmentName}</span>
                    <strong>× {eq.quantity}</strong>
                  </div>
                ))}
              </article>
            ))}
          </div>
        </>
      )}
    </div>
  );
}