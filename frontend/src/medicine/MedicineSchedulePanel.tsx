import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { fastingConflictText } from './MedicinePreviewPanel';

import {
  getPreferences,
  savePreferences,
  getSchedule,
} from './api';

import type {
  MedicinePreferences,
  ScheduledDose,
} from './types';

interface Props {
  offline: boolean;
  revision: number;
  onTimezone: (timezone: string) => void;
}

export default function MedicineSchedulePanel({
  offline,
  revision,
  onTimezone,
}: Props) {

  const [preferences, setPreferences] =
    useState<MedicinePreferences | null>(null);

  const [schedule, setSchedule] =
    useState<ScheduledDose[]>([]);

  const [date, setDate] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [busy, setBusy] = useState(false);
  const [ramadanEnabled, setRamadanEnabled] = useState(false);

  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (offline) return;

    let active = true;

    getPreferences()
      .then(data => {
        if (active) {
          setPreferences(data);
          setRamadanEnabled(Boolean(data.ramadan));
          onTimezone(data.timezone);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load meal-time preferences.');
        }
      });

    return () => {
      active = false;
    };
  }, [offline, onTimezone]);

  useEffect(() => {
    if (offline) return;

    let active = true;

    getSchedule(date || undefined)
      .then(data => {
        if (active) setSchedule(data);
      })
      .catch(() => {
        if (active) {
          setError('Unable to load medicine schedule.');
        }
      });

    return () => {
      active = false;
    };
  }, [offline, date, revision, refresh]);

  async function handleSave(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const form = new FormData(event.currentTarget);

    const data: MedicinePreferences = {
      breakfastTime: String(form.get('breakfastTime')),
      lunchTime: String(form.get('lunchTime')),
      dinnerTime: String(form.get('dinnerTime')),
      bedtime: String(form.get('bedtime')),
      timezone: preferences?.timezone || 'Asia/Dhaka',
      ramadan: ramadanEnabled ? {
        startDate: String(form.get('ramadanStart')),
        endDate: String(form.get('ramadanEnd')),
        sehriTime: String(form.get('sehriTime')),
        iftarTime: String(form.get('iftarTime')),
      } : null,
    };

    setBusy(true);
    setError('');
    setNotice('');

    try {
      const saved = await savePreferences(data);

      setPreferences(saved);
      setRefresh(previous => previous + 1);
      setNotice('Preferences saved. Schedule updated.');
    } catch {
      setError('Unable to save preferences.');
    } finally {
      setBusy(false);
    }
  }

  function formatTime(value: string) {
    return new Date(value).toLocaleString('en-GB', {
      timeZone: preferences?.timezone || 'Asia/Dhaka',
      day: 'numeric',
      month: 'short',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  }

  return (
    <>
      {error && (
        <p className="auth-alert" role="alert">
          {error}
        </p>
      )}

      {notice && (
        <p className="auth-success" role="status">
          {notice}
        </p>
      )}

      <section className="medicine-panel">
        <h2>Daily routine</h2>

        <p>
          Configure your meal times to personalize
          your medicine schedule.
        </p>

        {preferences && (
          <form onSubmit={handleSave}>
            <fieldset disabled={busy || offline}>

              <div className="medicine-fields">

                <label className="auth-field">
                  Breakfast
                  <input
                    type="time"
                    name="breakfastTime"
                    defaultValue={
                      preferences.breakfastTime.slice(0, 5)
                    }
                    required
                  />
                </label>

                <label className="auth-field">
                  Lunch
                  <input
                    type="time"
                    name="lunchTime"
                    defaultValue={
                      preferences.lunchTime.slice(0, 5)
                    }
                    required
                  />
                </label>

                <label className="auth-field">
                  Dinner
                  <input
                    type="time"
                    name="dinnerTime"
                    defaultValue={
                      preferences.dinnerTime.slice(0, 5)
                    }
                    required
                  />
                </label>

                <label className="auth-field">
                  Bedtime
                  <input
                    type="time"
                    name="bedtime"
                    defaultValue={
                      preferences.bedtime.slice(0, 5)
                    }
                    required
                  />
                </label>

              </div>

              <p>Timezone: {preferences.timezone}</p>

              <label className="medicine-checkbox">
                <input type="checkbox" checked={ramadanEnabled}
                  onChange={event => setRamadanEnabled(event.target.checked)} />
                Use Ramadan schedule for a date range
              </label>
              {ramadanEnabled && (
                <div className="medicine-fields">
                  <label className="auth-field">First fasting date
                    <input type="date" name="ramadanStart" defaultValue={preferences.ramadan?.startDate} required />
                  </label>
                  <label className="auth-field">Last fasting date
                    <input type="date" name="ramadanEnd" defaultValue={preferences.ramadan?.endDate} required />
                  </label>
                  <label className="auth-field">Sehri
                    <input type="time" name="sehriTime" defaultValue={preferences.ramadan?.sehriTime} required />
                  </label>
                  <label className="auth-field">Iftar
                    <input type="time" name="iftarTime" defaultValue={preferences.ramadan?.iftarTime} required />
                  </label>
                </div>
              )}

              <button type="submit" className="auth-primary">
                Save routine
              </button>

            </fieldset>
          </form>
        )}
      </section>

      <section className="medicine-panel">
        <h2>Daily medicine schedule</h2>

        <label className="auth-field">
          Schedule date (blank means today)
          <input
            type="date"
            value={date}
            onChange={event => setDate(event.target.value)}
            disabled={offline}
          />
        </label>

        {!offline && schedule.length === 0 && (
          <p>No scheduled doses for this date.</p>
        )}

        {offline && (
          <p>Offline — schedule may not be up to date.</p>
        )}

        {schedule.map(dose => (
          <article
            className="medicine-item"
            key={dose.doseId}
          >
            <h3>{dose.medicineName}</h3>

            {dose.scheduledAt && <strong>{formatTime(dose.scheduledAt)}</strong>}
            {dose.fastingConflict && <p className="medicine-warning">{fastingConflictText}</p>}

            <p>
              {dose.quantity} {dose.unit}
            </p>

            <p>Slot: {dose.slot}</p>

            {dose.critical && (
              <p>Critical medicine</p>
            )}
          </article>
        ))}
      </section>
    </>
  );
}