


import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import DosePatternPicker from './DosePatternPicker';
import MedicinePreviewPanel from './MedicinePreviewPanel';
import MedicinePrescriptionsPanel from './MedicinePrescriptionsPanel';
import MedicineSafetyWarnings from './MedicineSafetyWarnings';
import MedicineSchedulePanel from './MedicineSchedulePanel';

import {
  searchMedicines,
  getMyMedicines,
  addMedicine,
  stopMedicine,
  getPrescriptions,
  previewMedicine,
  replaceMedicine,
  getMedicineSafety,
  acknowledgeMedicineWarning,
} from './api';

import type {
  CatalogMedicine,
  UserMedicine,
  UserMedicineRequest,
  MealTiming,
  MedicinePrescription,
  MedicinePreview,
  MedicineSafetyReport,
} from './types';

import './medicine.css';

export default function MedicineCarePage({
  offline,
}: {
  offline: boolean;
}) {
  const [safety, setSafety] = useState<MedicineSafetyReport | null>(null);
  const [safetyError, setSafetyError] = useState('');
  const [safetyRevision, setSafetyRevision] = useState(0);
  const [catalog, setCatalog] = useState<CatalogMedicine[]>([]);
  const [medicines, setMedicines] = useState<UserMedicine[]>([]);
  const [scheduleRevision, setScheduleRevision] = useState(0);
  const [prescriptions, setPrescriptions] = useState<MedicinePrescription[]>([]);
  const [prescriptionId, setPrescriptionId] = useState('');
  const [timezone, setTimezone] = useState('Asia/Dhaka');
  const [editing, setEditing] = useState<UserMedicine | null>(null);
  const [dosePattern, setDosePattern] = useState('');
  const [formRevision, setFormRevision] = useState(0);
  const [pending, setPending] = useState<{ payload: UserMedicineRequest; preview: MedicinePreview } | null>(null);

  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [custom, setCustom] = useState(false);
  const [asNeeded, setAsNeeded] = useState(false);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (offline) return;

    let active = true;

    Promise.all([
      searchMedicines(),
      getMyMedicines(),
      getPrescriptions(),
    ])
      .then(([catalogData, medicineData, prescriptionData]) => {
        if (!active) return;

        setCatalog(catalogData);
        setMedicines(medicineData);
        setPrescriptions(prescriptionData);
        setError('');
      })
      .catch(() => {
        if (active) {
          setError('Unable to load medicine information.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [offline]);

  useEffect(() => {
    if (offline) return;
    let active = true;
    let latest = 0;
    function refreshSafety() {
      const attempt = ++latest;
      getMedicineSafety().then(report => {
        if (active && attempt === latest) {
          setSafety(report);
          setSafetyError('');
        }
      }).catch(() => {
        if (active && attempt === latest) setSafetyError('Unable to refresh medicine safety warnings.');
      });
    }
    refreshSafety();
    window.addEventListener('focus', refreshSafety);
    return () => {
      active = false;
      window.removeEventListener('focus', refreshSafety);
    };
  }, [offline, scheduleRevision, safetyRevision]);

  async function handleAcknowledge(id: string, intentional: boolean) {
    setBusy(true);
    setSafetyError('');
    try {
      await acknowledgeMedicineWarning(id, intentional);
    } catch {
      setSafetyError('Unable to record acknowledgement. The warning may have changed.');
    } finally {
      setSafetyRevision(previous => previous + 1);
      setBusy(false);
    }
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setBusy(true);
    setError('');

    try {
      const results = await searchMedicines(query);

      setCatalog(results);
      if (!results.some(item => item.id === selectedId)) setSelectedId(editing?.medicineId ?? '');
    } catch {
      setError('Unable to search medicines.');
    } finally {
      setBusy(false);
    }
  }

  async function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const data = new FormData(event.currentTarget);

    const duration = String(data.get('durationDays') || '');
    const quantity = String(data.get('remainingQuantity') || '');

    const payload: UserMedicineRequest = {
      medicineId: custom ? null : selectedId,
      customMedicineName: custom
        ? String(data.get('customMedicineName')).trim()
        : null,

      doseQuantity: Number(data.get('doseQuantity')),
      doseUnit: String(data.get('doseUnit')).trim(),

      dosePattern: asNeeded
        ? null
        : dosePattern,

      mealTiming: data.get('mealTiming') as MealTiming,
      asNeeded,
      startDate: String(data.get('startDate')),

      durationDays: duration ? Number(duration) : null,
      critical: data.get('critical') === 'on',
      remainingQuantity: quantity ? Number(quantity) : null,
      prescriptionId: prescriptionId || null,
    };

    if (!custom && !selectedId) {
      setError('Please select a medicine.');
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');

    try {
      const preview = await previewMedicine(payload, editing?.id ?? null);
      setPending({ payload, preview });
    } catch {
      setError('Unable to preview the medicine. Check the medicine, dates and dosage pattern.');
    } finally {
      setBusy(false);
    }
  }

  async function handleConfirm() {
    if (!pending || busy || offline) return;

    setBusy(true);
    setError('');
    try {
      if (editing) {
        await replaceMedicine(editing.id, pending.payload);
      } else {
        await addMedicine(pending.payload);
      }
      setPending(null);
      setNotice(editing ? 'Medicine changed. The previous record is in My Medicines.' : 'Medicine added.');
      resetForm();
      setScheduleRevision(previous => previous + 1);
      try {
        setMedicines(await getMyMedicines());
      } catch {
        setError('Medicine saved. Reload to see the updated list.');
      }
    } catch {
      setError('Unable to save the medicine. Check your details and try again.');
    } finally {
      setBusy(false);
    }
  }

  function resetForm(medicine: UserMedicine | null = null) {
    setEditing(medicine);
    setSelectedId(medicine?.medicineId ?? '');
    setCustom(medicine?.customMedicine ?? false);
    setAsNeeded(medicine?.asNeeded ?? false);
    setDosePattern(medicine?.dosePattern ?? '');
    if (medicine) setPrescriptionId(medicine.prescriptionId ?? '');
    setFormRevision(previous => previous + 1);
  }

  async function handleStop(
    event: FormEvent<HTMLFormElement>,
    id: string
  ) {
    event.preventDefault();

    const data = new FormData(event.currentTarget);
    const reason = String(data.get('reason') || '').trim();

    if (!reason) {
      setError('Please provide a reason.');
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');

    try {
      const updated = await stopMedicine(id, reason);

      setMedicines(previous =>
        previous.map(medicine =>
          medicine.id === id ? updated : medicine
        )
      );

      setScheduleRevision(previous => previous + 1);

      setNotice('Medicine stopped.');
    } catch {
      setError('Unable to stop medicine.');
    } finally {
      setBusy(false);
    }
  }

  const today = new Date().toLocaleDateString('sv-SE', {
    timeZone: timezone,
  });

  const remainingDays = editing?.durationDays == null ? '' : Math.max(
    1, editing.durationDays - (Date.parse(today) - Date.parse(editing.startDate)) / 86_400_000
  );

  const selected = catalog.find(
    medicine => medicine.id === selectedId
  );

  return (
    <div className="medicine-care">

      <p>Manage your medicines and prescribed dosage information.</p>
      <p className="medicine-note">
        Demo medicines are for development testing only.
      </p>

      {error && <p role="alert" className="auth-alert">{error}</p>}
      {notice && <p role="status" className="auth-success">{notice}</p>}
      {loading && <p role="status">Loading medicines...</p>}

      <section className="medicine-panel">
        <h2>Search medicine catalog</h2>

        <form onSubmit={handleSearch}>
          <fieldset disabled={busy || offline || loading || Boolean(pending)}>
            <label className="auth-field">
              Medicine name
              <input
                value={query}
                onChange={event => setQuery(event.target.value)}
                placeholder="Search Demo Alpha..."
              />
            </label>

            <button type="submit" className="auth-primary">
              Search
            </button>
          </fieldset>
        </form>
      </section>

      <section className="medicine-panel">
        <h2 id="medicine-editor">{editing ? 'Change ' + editing.medicineName : 'Add medicine'}</h2>
        {editing && <p>The new record applies from today. Earlier schedule dates keep the previous record.</p>}

        <form key={formRevision} onSubmit={handlePreview}>
          <fieldset disabled={busy || offline || loading || Boolean(pending)}>

            <label className="medicine-checkbox">
              <input
                type="checkbox"
                checked={custom}
                onChange={event => {
                  setCustom(event.target.checked);
                  setSelectedId('');
                }}
              />
              Add a custom medicine
            </label>

            {custom ? (
              <label className="auth-field">
                Medicine name
                <input
                  name="customMedicineName"
                  defaultValue={editing?.customMedicine ? editing.medicineName : ''}
                  maxLength={150}
                  required
                />
              </label>
            ) : (
              <label className="auth-field">
                Select medicine

                <select
                  value={selectedId}
                  onChange={event => setSelectedId(event.target.value)}
                  required
                >
                  <option value="">Choose a medicine</option>

                  {editing?.medicineId && !catalog.some(item => item.id === editing.medicineId) && (
                    <option value={editing.medicineId}>{editing.medicineName}</option>
                  )}
                  {catalog.map(medicine => (
                    <option key={medicine.id} value={medicine.id}>
                      {medicine.brandName} — {medicine.form}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {selected && !custom && (
              <div className="medicine-note">
                <strong>{selected.brandName}</strong>
                <p>{selected.manufacturer}</p>

                {selected.ingredients.map(ingredient => (
                  <p key={ingredient.id}>
                    {ingredient.name} — {ingredient.strength}{' '}
                    {ingredient.strengthUnit}
                  </p>
                ))}
              </div>
            )}

            <div className="medicine-fields">
              <label className="auth-field">
                Base dose quantity
                <input
                  name="doseQuantity"
                  type="number"
                  min="0.001"
                  step="0.001"
                  defaultValue={editing?.doseQuantity ?? 1}
                  required
                />
              </label>

              <label className="auth-field">
                Dose unit
                <input
                  name="doseUnit"
                  defaultValue={editing?.doseUnit ?? ''}
                  placeholder="tablet, ml, capsule..."
                  maxLength={30}
                  required
                />
              </label>
            </div>

            <label className="medicine-checkbox">
              <input
                type="checkbox"
                checked={asNeeded}
                onChange={event => setAsNeeded(event.target.checked)}
              />
              Take only when needed
            </label>

            {!asNeeded && (
              <>
                <label className="auth-field">
                  Dose pattern (multiplier per slot)
                  <input name="dosePattern" placeholder="1+0+1" maxLength={50}
                    value={dosePattern} onChange={event => setDosePattern(event.target.value)} required />
                  <small>Morning + Noon + Night + Optional bedtime</small>
                </label>
                <DosePatternPicker value={dosePattern} onChange={setDosePattern} />
              </>
            )}

            <label className="auth-field">
              Meal timing
              <select name="mealTiming" defaultValue={editing?.mealTiming}>
                <option value="ANYTIME">Anytime</option>
                <option value="BEFORE_MEAL">Before meals</option>
                <option value="AFTER_MEAL">After meals</option>
              </select>
            </label>

            <div className="medicine-fields">
              <label className="auth-field">
                Start date
                <input
                  name="startDate"
                  type="date"
                  defaultValue={today}
                  readOnly={Boolean(editing)}
                  required
                />
              </label>

              <label className="auth-field">
                Duration in days
                <input
                  name="durationDays"
                  type="number"
                  min="1"
                  max="36500"
                  defaultValue={remainingDays}
                  placeholder="Blank for ongoing"
                />
              </label>
            </div>

            <label className="auth-field">
              Remaining quantity (optional)
              <input
                name="remainingQuantity"
                defaultValue={editing?.remainingQuantity ?? ''}
                type="number"
                min="0"
                step="0.001"
              />
            </label>

            <label className="medicine-checkbox">
              <input name="critical" type="checkbox" defaultChecked={editing?.critical} />
              Critical medicine
            </label>

            <label className="auth-field">
              Prescription (optional)
              <select value={prescriptionId} onChange={event => setPrescriptionId(event.target.value)}>
                <option value="">No prescription</option>
                {prescriptions.map(prescription => (
                  <option key={prescription.id} value={prescription.id}>
                    {prescription.doctorName} · {prescription.prescriptionDate}
                  </option>
                ))}
              </select>
            </label>
            <div className="medicine-actions">
              <button className="auth-primary" type="submit">Preview medicine</button>
              {editing && <button type="button" onClick={() => resetForm()}>Cancel change</button>}
            </div>

          </fieldset>
        </form>
        {pending && (
          <MedicinePreviewPanel preview={pending.preview} timezone={timezone} disabled={busy || offline}
            onSave={() => void handleConfirm()} onBack={() => setPending(null)} />
        )}
      </section>

      <MedicinePrescriptionsPanel prescriptions={prescriptions} medicines={medicines}
        disabled={busy || loading || offline || Boolean(pending)} today={today}
        onCreate={prescription => {
          setPrescriptions(previous => [prescription, ...previous]);
          setPrescriptionId(prescription.id);
        }} />

      <section className="medicine-panel">
        <h2>My Medicines</h2>
        {safetyError && <p role="alert" className="auth-alert">{safetyError}</p>}
        {safety && !safety.allergyCheckAvailable && <p>Health Passport allergy checking is unavailable.</p>}
        {medicines.length > 0 && <button type="button" disabled={busy || offline}
          onClick={() => setSafetyRevision(previous => previous + 1)}>Refresh safety checks</button>}

        {!loading && medicines.length === 0 && (
          <p>No medicines added yet.</p>
        )}

        {medicines.map(medicine => {
          const warnings = safety?.warnings.filter(warning =>
            warning.medicineId === medicine.id || warning.otherMedicineId === medicine.id
          ) ?? [];
          return (
          <article className="medicine-item" key={medicine.id}>
            <h3>{medicine.medicineName}</h3>

            <p>Status: <strong>{medicine.status}</strong></p>

            <p>
              Base dose: {medicine.doseQuantity} {medicine.doseUnit}
            </p>

            <p>
              Pattern: {medicine.asNeeded
                ? 'As needed'
                : medicine.dosePattern}
            </p>

            <p>Timing: {medicine.mealTiming.replaceAll('_', ' ')}</p>
            <p>Start: {medicine.startDate}</p>

            <p>
              Duration: {medicine.durationDays === null
                ? 'Ongoing'
                : `${medicine.durationDays} days`}
            </p>

            {medicine.remainingQuantity !== null && (
              <p>Remaining: {medicine.remainingQuantity}</p>
            )}

            {medicine.critical && <p>Critical medicine</p>}

            {medicine.customMedicine && !warnings.some(warning => warning.kind === 'UNCHECKABLE') && (
              <p>Custom medicine — safety checking unavailable.</p>
            )}
            <MedicineSafetyWarnings warnings={warnings} disabled={busy || offline}
              onAcknowledge={(id, intentional) => void handleAcknowledge(id, intentional)} />

            {medicine.previousMedicineId && (
              <p>Replaces {medicines.find(item => item.id === medicine.previousMedicineId)?.medicineName ?? 'a previous record'}.</p>
            )}
            {medicine.scheduleEndDate && <p>Schedule ended on {medicine.scheduleEndDate}.</p>}

            {medicine.stopReason && (
              <p>Stop reason: {medicine.stopReason}</p>
            )}

            {medicine.status === 'ACTIVE' && (
              <button type="button" disabled={busy || offline || Boolean(pending) || medicine.startDate > today}
                onClick={() => {
                  resetForm(medicine);
                  document.getElementById('medicine-editor')?.scrollIntoView();
                }}>Change medicine</button>
            )}
            {medicine.status === 'ACTIVE' && (
              <form onSubmit={event => handleStop(event, medicine.id)}>
                <fieldset disabled={busy || offline || Boolean(pending)}>
                  <label className="auth-field">
                    Reason for stopping
                    <input
                      name="reason"
                      maxLength={255}
                      required
                    />
                  </label>

                  <button type="submit">
                    Record as stopped
                  </button>
                </fieldset>
              </form>
            )}
          </article>
          );
        })}
      </section>
      <MedicineSchedulePanel offline={offline} revision={scheduleRevision} onTimezone={setTimezone} />

    </div>
  );
}