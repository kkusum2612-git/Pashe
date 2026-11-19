import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { createBloodRequest, getBloodRequests } from './api';
import type {
  BloodGroup,
  BloodRequest,
  BloodRequestStatus,
  BloodRequestUrgency
} from './types';
import './bloodRequests.css';

const bloodGroups: BloodGroup[] = [
  'A+', 'A-', 'B+', 'B-',
  'AB+', 'AB-', 'O+', 'O-'
];

function toDateTimeLocal(value: Date) {
  const offset = value.getTimezoneOffset();
  const local = new Date(value.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
}

export default function BloodRequestPage({
  bn = false,
  offline = false
}: {
  bn?: boolean;
  offline?: boolean;
}) {
  const t = (en: string, bangla: string) =>
    bn ? bangla : en;

  const [bloodGroup, setBloodGroup] = useState<BloodGroup>('O+');
  const [requiredUnits, setRequiredUnits] = useState('1');
  const [hospitalName, setHospitalName] = useState('');
  const [urgency, setUrgency] =
    useState<BloodRequestUrgency>('URGENT');
  const [expiresAt, setExpiresAt] = useState('');

  const [requests, setRequests] = useState<BloodRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [saveFailed, setSaveFailed] = useState(false);
  const [saved, setSaved] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const minimumDeadline = useMemo(
    () => toDateTimeLocal(new Date()),
    []
  );

  useEffect(() => {
    let active = true;

    if (offline) {
      return () => {
        active = false;
      };
    }

    const refresh = () => {
      getBloodRequests()
        .then(data => {
          if (!active) return;
          setRequests(data);
          setLoadFailed(false);
        })
        .catch(() => {
          if (active) setLoadFailed(true);
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    };

    refresh();

    const interval = window.setInterval(refresh, 15000);

    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [offline, refreshKey]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (busy || offline || !hospitalName.trim() || !requiredUnits.trim() || !Number.isInteger(Number(requiredUnits)) || Number(requiredUnits) < 1) return;

    const deadline = new Date(expiresAt);

    if (
      Number.isNaN(deadline.getTime()) ||
      deadline.getTime() <= Date.now()
    ) {
      setSaveFailed(true);
      setSaved(false);
      return;
    }

    setBusy(true);
    setSaveFailed(false);
    setSaved(false);

    try {
      const created = await createBloodRequest({
        bloodGroup,
        requiredUnits: Number(requiredUnits),
        hospitalName: hospitalName.trim(),
        urgency,
        expiresAt: deadline.toISOString()
      });

      setRequests(previous => [created, ...previous]);
      setBloodGroup('O+');
      setRequiredUnits('1');
      setHospitalName('');
      setUrgency('URGENT');
      setExpiresAt('');
      setSaved(true);
    } catch {
      setSaveFailed(true);
    } finally {
      setBusy(false);
    }
  }

  function formatDate(value: string) {
    return new Date(value).toLocaleString(
      bn ? 'bn-BD' : 'en-BD',
      {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit'
      }
    );
  }

  function statusLabel(status: BloodRequestStatus) {
    switch (status) {
      case 'FULFILLED':
        return t('Fulfilled', 'সম্পন্ন');
      case 'EXPIRED':
        return t('Expired', 'মেয়াদোত্তীর্ণ');
      default:
        return t('Active', 'সক্রিয়');
    }
  }

  function urgencyLabel(level: BloodRequestUrgency) {
    switch (level) {
      case 'CRITICAL':
        return t('Critical', 'অতি জরুরি');
      case 'NORMAL':
        return t('Normal', 'সাধারণ');
      default:
        return t('Urgent', 'জরুরি');
    }
  }

  return (
    <div className="blood-requests">
      <section className="blood-requests-hero">
        <div>
          <h2>{t('Blood Requests', 'রক্তের অনুরোধ')}</h2>
          <p>
            {t(
              'Create requests, monitor request status, and track donor progress from one place.',
              'এক জায়গা থেকে রক্তের অনুরোধ তৈরি করুন, স্ট্যাটাস দেখুন এবং দাতার অগ্রগতি পর্যবেক্ষণ করুন।'
            )}
          </p>
        </div>

        <button
          type="button"
          className="blood-requests-refresh"
          disabled={offline}
          onClick={() => setRefreshKey(value => value + 1)}
        >
          {t('Refresh', 'রিফ্রেশ')}
        </button>
      </section>

      {offline && (
        <p className="auth-alert" role="status">
          {t(
            'You are offline. Connect to create or update requests.',
            'আপনি অফলাইনে আছেন। অনুরোধ তৈরি বা আপডেট করতে ইন্টারনেটে যুক্ত হন।'
          )}
        </p>
      )}

      <div className="blood-requests-layout">
        <section className="blood-requests-panel">
          <div className="blood-requests-section-head">
            <h3>{t('Create a blood request', 'নতুন রক্তের অনুরোধ')}</h3>
            <p className="auth-hint">
              {t(
                'Fill in the request details carefully so donors can respond appropriately.',
                'সঠিকভাবে তথ্য পূরণ করুন যাতে দাতারা যথাযথভাবে সাড়া দিতে পারেন।'
              )}
            </p>
          </div>

          {saved && (
            <p className="auth-success" role="status">
              {t(
                'Blood request created successfully.',
                'রক্তের অনুরোধ সফলভাবে তৈরি হয়েছে।'
              )}
            </p>
          )}

          {saveFailed && (
            <p className="auth-alert" role="alert">
              {t(
                'Could not create the request. Please review the information and try again.',
                'অনুরোধ তৈরি করা যায়নি। তথ্য যাচাই করে আবার চেষ্টা করুন।'
              )}
            </p>
          )}

          <form onSubmit={submit}>
            <fieldset
              disabled={busy || offline}
              className="blood-requests-fieldset"
            >
              <div className="blood-requests-form-grid">
                <label className="auth-field">
                  <span>{t('Blood group', 'রক্তের গ্রুপ')}</span>
                  <select
                    value={bloodGroup}
                    onChange={event =>
                      setBloodGroup(event.target.value as BloodGroup)
                    }
                    required
                  >
                    {bloodGroups.map(group => (
                      <option key={group} value={group}>
                        {group}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="auth-field">
                  <span>{t('Required units', 'প্রয়োজনীয় ইউনিট')}</span>
                  <input
                    type="number"
                    min={1}
                    step={1}
                    value={requiredUnits}
                    onChange={event =>
                      setRequiredUnits(event.target.value)
                    }
                    required
                  />
                </label>

                <label className="auth-field blood-request-span-2">
                  <span>{t('Hospital name', 'হাসপাতালের নাম')}</span>
                  <input
                    value={hospitalName}
                    onChange={event => setHospitalName(event.target.value)}
                    maxLength={200}
                    placeholder={t(
                      'Dhaka Medical College Hospital',
                      'ঢাকা মেডিকেল কলেজ হাসপাতাল'
                    )}
                    required
                  />
                </label>

                <label className="auth-field">
                  <span>{t('Urgency', 'জরুরি অবস্থা')}</span>
                  <select
                    value={urgency}
                    onChange={event =>
                      setUrgency(event.target.value as BloodRequestUrgency)
                    }
                  >
                    <option value="NORMAL">
                      {t('Normal', 'সাধারণ')}
                    </option>
                    <option value="URGENT">
                      {t('Urgent', 'জরুরি')}
                    </option>
                    <option value="CRITICAL">
                      {t('Critical', 'অতি জরুরি')}
                    </option>
                  </select>
                </label>

                <label className="auth-field">
                  <span>{t('Needed before', 'প্রয়োজনের শেষ সময়')}</span>
                  <input
                    type="datetime-local"
                    value={expiresAt}
                    min={minimumDeadline}
                    onChange={event => setExpiresAt(event.target.value)}
                    required
                  />
                </label>
              </div>

              <div className="blood-requests-actions">
                <button
                  className="auth-primary"
                  type="submit"
                  disabled={
                    busy ||
                    offline ||
                    !hospitalName.trim() ||
                    !expiresAt ||
                    !requiredUnits.trim() || !Number.isInteger(Number(requiredUnits)) || Number(requiredUnits) < 1
                  }
                >
                  {busy
                    ? t('Creating...', 'তৈরি হচ্ছে...')
                    : t('Create blood request', 'রক্তের অনুরোধ তৈরি করুন')}
                </button>
              </div>
            </fieldset>
          </form>
        </section>

        <section className="blood-requests-panel">
          <div className="blood-requests-section-head">
            <h3>{t('My blood requests', 'আমার রক্তের অনুরোধ')}</h3>
            <p className="auth-hint">
              {t(
                'Track request progress, accepted donors, and confirmed units.',
                'অনুরোধের অগ্রগতি, সম্মত দাতা এবং নিশ্চিত ইউনিট পর্যবেক্ষণ করুন।'
              )}
            </p>
          </div>

          {loading && !offline && (
            <p role="status">
              {t('Loading requests...', 'অনুরোধ লোড হচ্ছে...')}
            </p>
          )}

          {loadFailed && (
            <p className="auth-alert" role="alert">
              {t(
                'Unable to refresh requests right now. Showing the latest available data.',
                'এই মুহূর্তে অনুরোধ রিফ্রেশ করা যাচ্ছে না। সর্বশেষ উপলব্ধ তথ্য দেখানো হচ্ছে।'
              )}
            </p>
          )}

          {!loading && requests.length === 0 && !loadFailed && (
            <div className="blood-requests-empty">
              <h4>{t('No requests yet', 'এখনো কোনো অনুরোধ নেই')}</h4>
              <p>
                {t(
                  'Create your first blood request to start tracking it here.',
                  'প্রথম রক্তের অনুরোধ তৈরি করুন, তারপর এখানেই তার অগ্রগতি দেখতে পারবেন।'
                )}
              </p>
            </div>
          )}

          <div className="blood-requests-list">
            {requests.map(item => (
              <article className="blood-request-card" key={item.id}>
                <div className="blood-request-card-top">
                  <div className="blood-request-blood-group">
                    {item.bloodGroup}
                  </div>

                  <div className="blood-request-badges">
                    <span className={`blood-request-badge status-${item.status.toLowerCase()}`}>
                      {statusLabel(item.status)}
                    </span>
                    <span className={`blood-request-badge urgency-${item.urgency.toLowerCase()}`}>
                      {urgencyLabel(item.urgency)}
                    </span>
                  </div>
                </div>

                <h4>{item.hospitalName}</h4>

                <div className="blood-request-meta">
                  <p>
                    <strong>{t('Needed before', 'শেষ সময়')}:</strong>{' '}
                    {formatDate(item.expiresAt)}
                  </p>
                  <p>
                    <strong>{t('Required units', 'প্রয়োজনীয় ইউনিট')}:</strong>{' '}
                    {item.requiredUnits}
                  </p>
                </div>

                <div className="blood-request-counts">
                  <div className="blood-request-stat">
                    <span>{t('Accepted donors', 'সম্মত দাতা')}</span>
                    <strong>{item.acceptedDonorsCount}</strong>
                  </div>

                  <div className="blood-request-stat">
                    <span>{t('Confirmed units', 'নিশ্চিত ইউনিট')}</span>
                    <strong>
                      {item.confirmedUnits} / {item.requiredUnits}
                    </strong>
                  </div>
                </div>

                <div className="blood-request-progress-block">
                  <div className="blood-request-progress-label">
                    <span>{t('Collection progress', 'সংগ্রহের অগ্রগতি')}</span>
                    <strong>
                      {Math.min(item.confirmedUnits, item.requiredUnits)} / {item.requiredUnits}
                    </strong>
                  </div>

                  <progress
                    className="blood-request-progress"
                    max={Math.max(1, item.requiredUnits)}
                    value={Math.min(
                      item.requiredUnits,
                      Math.max(0, item.confirmedUnits)
                    )}
                    aria-label={t(
                      'Confirmed blood units',
                      'নিশ্চিত রক্তের ইউনিট'
                    )}
                  />
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}