import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { getDonorProfile, saveDonorProfile } from './api';
import type { BloodGroup } from './api';
import './donor.css';

const bloodGroups: BloodGroup[] = [
  'A+', 'A-', 'B+', 'B-',
  'AB+', 'AB-', 'O+', 'O-'
];

export default function DonorProfilePage({
  bn = false,
  offline = false,
}: {
  bn?: boolean;
  offline?: boolean;
}) {
  const t = (en: string, bangla: string) => bn ? bangla : en;

  const [bloodGroup, setBloodGroup] =
    useState<BloodGroup | ''>('');

  const [area, setArea] = useState('');
  const [lastDonationDate, setLastDonationDate] = useState('');
  const [available, setAvailable] = useState(false);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [saveFailed, setSaveFailed] = useState(false);
  const [saved, setSaved] = useState(false);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;

    getDonorProfile()
      .then(profile => {
        if (!active || !profile) return;

        setBloodGroup(profile.bloodGroup);
        setArea(profile.area);
        setLastDonationDate(profile.lastDonationDate ?? '');
        setAvailable(profile.available);
      })
      .catch(() => {
        if (active) setLoadFailed(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [attempt]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!bloodGroup || !area.trim() || busy || offline) return;

    setBusy(true);
    setSaveFailed(false);
    setSaved(false);

    try {
      const profile = await saveDonorProfile({
        bloodGroup,
        area: area.trim(),
        lastDonationDate: lastDonationDate || null,
        available,
      });

      setBloodGroup(profile.bloodGroup);
      setArea(profile.area);
      setLastDonationDate(profile.lastDonationDate ?? '');
      setAvailable(profile.available);
      setSaved(true);
    } catch {
      setSaveFailed(true);
    } finally {
      setBusy(false);
    }
  }

  const now = new Date();
  const today = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');

  if (loading) {
    return (
      <p role="status">
        {t('Loading donor profile...', 'রক্তদাতার প্রোফাইল লোড হচ্ছে...')}
      </p>
    );
  }

  if (loadFailed) {
    return (
      <>
        <p className="auth-alert" role="alert">
          {t('Unable to load donor profile.', 'প্রোফাইল লোড করা যায়নি।')}
        </p>

        <button
          className="auth-primary"
          disabled={offline}
          onClick={() => {
            setLoadFailed(false);
            setLoading(true);
            setAttempt(value => value + 1);
          }}
        >
          {t('Try again', 'আবার চেষ্টা করুন')}
        </button>
      </>
    );
  }

  return (
    <form className="donor-form" onSubmit={submit}>
      <p className="auth-hint">
        {t(
          'Join the Blood Network by providing your donor information.',
          'রক্তদাতার তথ্য দিয়ে রক্তদান নেটওয়ার্কে যোগ দিন।'
        )}
      </p>

      {saved && (
        <p className="auth-success" role="status">
          {t(
            'Donor profile saved successfully.',
            'রক্তদাতার প্রোফাইল সংরক্ষণ করা হয়েছে।'
          )}
        </p>
      )}

      {saveFailed && (
        <p className="auth-alert" role="alert">
          {t(
            'Unable to save your profile. Please check your details and try again.',
            'প্রোফাইল সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।'
          )}
        </p>
      )}

      <fieldset disabled={busy || offline}>
        <label className="auth-field">
          {t('Blood group', 'রক্তের গ্রুপ')}

          <select
            value={bloodGroup}
            onChange={event =>
              setBloodGroup(event.target.value as BloodGroup | '')
            }
            required
          >
            <option value="">
              {t('Select blood group', 'রক্তের গ্রুপ নির্বাচন করুন')}
            </option>

            {bloodGroups.map(group => (
              <option key={group} value={group}>
                {group}
              </option>
            ))}
          </select>
        </label>

        <label className="auth-field">
          {t('Area', 'এলাকা')}

          <input
            value={area}
            onChange={event => setArea(event.target.value)}
            maxLength={120}
            placeholder="Mirpur, Dhaka"
            required
          />
        </label>

        <label className="auth-field">
          {t('Last donation date', 'শেষ রক্তদানের তারিখ')}

          <input
            type="date"
            value={lastDonationDate}
            max={today}
            onChange={event =>
              setLastDonationDate(event.target.value)
            }
          />
        </label>

        <p className="auth-hint">
          {t(
            'Leave this empty if you have never donated blood.',
            'আগে রক্তদান না করে থাকলে খালি রাখুন।'
          )}
        </p>

        <label className="donor-availability">
          <input
            type="checkbox"
            checked={available}
            onChange={event => setAvailable(event.target.checked)}
          />

          <span>
            {t(
              'Available for blood donation requests',
              'রক্তদানের অনুরোধ গ্রহণ করতে প্রস্তুত'
            )}
          </span>
        </label>

        <button
          type="submit"
          className="auth-primary"
          disabled={busy || offline || !bloodGroup || !area.trim()}
        >
          {busy
            ? t('Saving...', 'সংরক্ষণ হচ্ছে...')
            : t('Save donor profile', 'প্রোফাইল সংরক্ষণ করুন')}
        </button>
      </fieldset>
    </form>
  );
}