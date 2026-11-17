import { useEffect, useRef, useState } from 'react';
import { ApiError } from '../../auth/api';
import { ambulanceApi } from './api';

import type {
  AmbulanceProfile,
  RegistrationRequest
} from './types';

import './ambulance.css';

function VehicleFields({
  initial
}: {
  initial?: RegistrationRequest
}) {
  return (
    <div className="am-form-grid">
      <label>
        Registration number
        <input
          name="registrationNumber"
          defaultValue={initial?.registrationNumber}
          maxLength={50}
          required
        />
      </label>

      <label>
        Vehicle name
        <input
          name="vehicleName"
          defaultValue={initial?.vehicleName}
          maxLength={100}
          required
        />
      </label>

      <label>
        Vehicle type
        <input
          name="vehicleType"
          defaultValue={initial?.vehicleType}
          placeholder="e.g. ICU, Basic Life Support"
          maxLength={50}
          required
        />
      </label>
    </div>
  );
}

function readVehicle(form: HTMLFormElement): RegistrationRequest {
  const data = new FormData(form);

  return {
    registrationNumber: String(data.get('registrationNumber')).trim(),
    vehicleName: String(data.get('vehicleName')).trim(),
    vehicleType: String(data.get('vehicleType')).trim()
  };
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      return 'Duplicate information or an invalid verification state.';
    }

    if (error.status === 403) {
      return 'You do not have permission to perform this action.';
    }

    if (error.status === 400 || error.status === 413) {
      return 'Check the information or uploaded file.';
    }
  }

  return 'The request could not be completed. Please try again.';
}

export default function OperatorWorkspace({
  offline = false
}: {
  offline?: boolean
}) {
  const [profiles, setProfiles] = useState<AmbulanceProfile[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const pending = useRef(false);

  const selected =
    profiles.find(item => item.id === selectedId) ?? profiles[0];

  useEffect(() => {
    let active = true;

    ambulanceApi.getMyAmbulances()
      .then(items => {
        if (active) setProfiles(items);
      })
      .catch(err => {
        if (active) setError(errorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  function saveProfile(profile: AmbulanceProfile) {
    setProfiles(current => {
      const exists = current.some(item => item.id === profile.id);

      return exists
        ? current.map(item => item.id === profile.id ? profile : item)
        : [profile, ...current];
    });

    setSelectedId(profile.id);
  }

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
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  if (loading) {
    return <p role="status">Loading your ambulances...</p>;
  }

  return (
    <div className="am-workspace" aria-busy={busy}>

      <div className="am-heading">
        <div>
          <h3>Your fleet</h3>
          <p>Manage your registered ambulances and verification.</p>
        </div>

        <button
          type="button"
          className="am-primary"
          disabled={busy || offline}
          onClick={() => setShowRegister(value => !value)}
        >
          {showRegister ? 'Cancel' : '+ Register ambulance'}
        </button>
      </div>

      {error && <p className="auth-alert" role="alert">{error}</p>}
      {notice && <p className="auth-success" role="status">{notice}</p>}

      {showRegister && (
        <section className="am-panel">
          <h3>Register a vehicle</h3>

          <form onSubmit={event => {
            event.preventDefault();

            const form = event.currentTarget;
            const data = readVehicle(form);

            void perform(async () => {
              saveProfile(await ambulanceApi.register(data));
              setShowRegister(false);
            }, 'Ambulance registered successfully.');
          }}>
            <fieldset disabled={busy || offline}>
              <VehicleFields />

              <button className="am-primary" type="submit">
                Register vehicle
              </button>
            </fieldset>
          </form>
        </section>
      )}

      <div className="am-layout">

        <aside className="am-panel">
          <h3>Registered vehicles ({profiles.length})</h3>

          {profiles.length === 0 && (
            <p className="am-muted">
              No ambulances registered yet.
            </p>
          )}

          <div className="am-vehicle-list">
            {profiles.map(profile => (
              <button
                key={profile.id}
                type="button"
                className={
                  'am-vehicle-item' +
                  (selected?.id === profile.id ? ' am-selected' : '')
                }
                onClick={() => setSelectedId(profile.id)}
                disabled={busy}
              >
                <strong>{profile.vehicleName}</strong>
                <span>{profile.registrationNumber}</span>

                <span
                  className={
                    `am-status am-${profile.verificationStatus.toLowerCase()}`
                  }
                >
                  {profile.verificationStatus}
                </span>
              </button>
            ))}
          </div>
        </aside>

        {selected && (
          <div className="am-details">

            <section className="am-panel">
              <div className="am-heading">
                <h3>{selected.vehicleName}</h3>

                <span
                  className={
                    `am-status am-${selected.verificationStatus.toLowerCase()}`
                  }
                >
                  {selected.verificationStatus}
                </span>
              </div>

              {selected.reviewNotes && (
                <p className="am-muted">
                  Review notes: {selected.reviewNotes}
                </p>
              )}

              <form
                key={selected.id}
                onSubmit={event => {
                  event.preventDefault();

                  const data = readVehicle(event.currentTarget);

                  void perform(async () => {
                    saveProfile(
                      await ambulanceApi.update(selected.id, data)
                    );
                  }, 'Vehicle updated. Verification is required again.');
                }}
              >
                <fieldset disabled={busy || offline}>
                  <VehicleFields initial={selected} />

                  <button className="am-secondary" type="submit">
                    Save vehicle details
                  </button>
                </fieldset>
              </form>
            </section>

            <section className="am-panel">
              <h3>Medical equipment</h3>

              {selected.equipment.length === 0 && (
                <p className="am-muted">No equipment added.</p>
              )}

              {selected.equipment.map(item => (
                <div className="am-list-row" key={item.id}>
                  <span>
                    {item.equipmentName} × {item.quantity}
                  </span>

                  <button
                    className="am-danger"
                    type="button"
                    disabled={busy || offline}
                    onClick={() => {
                      if (!window.confirm('Remove this equipment?')) return;

                      void perform(async () => {
                        saveProfile(await ambulanceApi.removeEquipment(
                          selected.id, item.id
                        ));
                      }, 'Equipment removed.');
                    }}
                  >
                    Remove
                  </button>
                </div>
              ))}

              <form onSubmit={event => {
                event.preventDefault();

                const form = event.currentTarget;
                const data = new FormData(form);

                void perform(async () => {
                  saveProfile(await ambulanceApi.addEquipment(
                    selected.id,
                    String(data.get('equipmentName')).trim(),
                    Number(data.get('quantity'))
                  ));

                  form.reset();
                }, 'Equipment added.');
              }}>
                <fieldset disabled={busy || offline}>
                  <div className="am-form-grid">
                    <label>
                      Equipment name
                      <input
                        name="equipmentName"
                        maxLength={100}
                        required
                      />
                    </label>

                    <label>
                      Quantity
                      <input
                        name="quantity"
                        type="number"
                        min={1}
                        max={2147483647}
                        defaultValue={1}
                        required
                      />
                    </label>
                  </div>

                  <button type="submit" className="am-primary">
                    Add equipment
                  </button>
                </fieldset>
              </form>
            </section>

            <section className="am-panel">
              <h3>License documents</h3>

              {selected.documents.length === 0 && (
                <p className="am-muted">No documents uploaded.</p>
              )}

              {selected.documents.map(doc => (
                <div className="am-list-row" key={doc.id}>
                  <span>{doc.fileName}</span>

                  <div className="am-actions">
                    <button
                      className="am-secondary"
                      type="button"
                      disabled={busy || offline}
                      onClick={() => void perform(() =>
                        ambulanceApi.downloadDocument(
                          selected.id, doc.id, doc.fileName
                        )
                      )}
                    >
                      Download
                    </button>

                    <button
                      className="am-danger"
                      type="button"
                      disabled={busy || offline}
                      onClick={() => {
                        if (!window.confirm('Remove this document?')) return;

                        void perform(async () => {
                          saveProfile(await ambulanceApi.removeDocument(
                            selected.id, doc.id
                          ));
                        }, 'Document removed.');
                      }}
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))}

              <form onSubmit={event => {
                event.preventDefault();

                const form = event.currentTarget;
                const file = new FormData(form).get('file');

                if (!(file instanceof File) ||
                    file.size === 0 ||
                    file.size > 5 * 1024 * 1024) {
                  setError('Select a file no larger than 5 MB.');
                  return;
                }

                void perform(async () => {
                  saveProfile(await ambulanceApi.uploadDocument(
                    selected.id, file
                  ));

                  form.reset();
                }, 'License document uploaded.');
              }}>
                <fieldset disabled={busy || offline}>
                  <label>
                    Upload license
                    <input
                      name="file"
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg"
                      required
                    />
                  </label>

                  <p className="am-muted">
                    PDF, PNG or JPEG. Maximum 5 MB.
                  </p>

                  <button type="submit" className="am-primary">
                    Upload document
                  </button>
                </fieldset>
              </form>

              <p className="am-muted">
                Changing documents or equipment requires administrator
                verification again.
              </p>
            </section>

            <section className="am-panel">
              <h3>Availability</h3>

              <p>
                Current status: <strong>
                  {selected.available ? 'Available' : 'Unavailable'}
                </strong>
              </p>

              {selected.verificationStatus !== 'APPROVED' && (
                <p className="am-muted">
                  Your ambulance must be approved before becoming available.
                </p>
              )}

              <button
                className="am-primary"
                type="button"
                disabled={
                  busy ||
                  offline ||
                  selected.verificationStatus !== 'APPROVED'
                }
                onClick={() => void perform(async () => {
                  saveProfile(await ambulanceApi.setAvailability(
                    selected.id, !selected.available
                  ));
                }, 'Availability updated.')}
              >
                {selected.available ? 'Set unavailable' : 'Set available'}
              </button>
            </section>

          </div>
        )}
      </div>
    </div>
  );
}