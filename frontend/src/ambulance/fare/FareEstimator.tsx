import { useId, useState } from 'react';
import { useLanguage } from '../../shared/i18n/useLanguage';
import { fareApi } from './api';
import type { FareLocation, FareResponse } from './types';
import './fare.css';

type Translate = (en: string, bn: string) => string;

interface LocationPickerProps {
  label: string;
  selected: FareLocation | null;
  onSelect: (location: FareLocation | null) => void;
  disabled: boolean;
  t: Translate;
}

function LocationPicker({
  label,
  selected,
  onSelect,
  disabled,
  t
}: LocationPickerProps) {

  const id = useId();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<FareLocation[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState(false);

  async function search() {

    if (query.trim().length < 3 || loading || disabled) return;

    setLoading(true);
    setError(false);
    setSearched(false);
    setResults([]);
    onSelect(null);

    try {
      setResults(await fareApi.searchLocations(query.trim()));
      setSearched(true);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }

  function selectLocation(location: FareLocation) {
    setQuery(location.label);
    setResults([]);
    setSearched(false);
    onSelect(location);
  }

  return (
    <div className="fare-location">
      <label htmlFor={id}>{label}</label>

      <div className="fare-search-row">

        <input
          id={id}
          value={query}
          maxLength={150}
          disabled={disabled || loading}
          placeholder={t(
            'Enter an area or hospital',
            'এলাকা বা হাসপাতালের নাম লিখুন'
          )}
          onChange={event => {
            setQuery(event.target.value);
            setResults([]);
            setSearched(false);
            setError(false);
            onSelect(null);
          }}
          onKeyDown={event => {
            if (event.key === 'Enter') {
              event.preventDefault();
              void search();
            }
          }}
        />

        <button
          type="button"
          className="am-secondary"
          disabled={
            disabled || loading || query.trim().length < 3
          }
          onClick={() => void search()}
        >
          {loading
            ? t('Searching...', 'খোঁজা হচ্ছে...')
            : t('Search', 'খুঁজুন')}
        </button>
      </div>

      {error && (
        <p className="auth-alert" role="alert">
          {t(
            'Unable to search locations. Please try again.',
            'স্থান খুঁজে পাওয়া যায়নি। আবার চেষ্টা করুন।'
          )}
        </p>
      )}

      {searched && results.length === 0 && (
        <p className="am-muted" role="status">
          {t('No matching locations found.', 'কোনো স্থান পাওয়া যায়নি।')}
        </p>
      )}

      {results.length > 0 && (
        <ul className="fare-results">
          {results.map((location, index) => (
            <li key={`${location.label}-${index}`}>
              <button
                type="button"
                className="fare-location-option"
                disabled={disabled}
                onClick={() => selectLocation(location)}
              >
                {location.label}
              </button>
            </li>
          ))}
        </ul>
      )}

      {selected && (
        <p className="fare-selected" role="status">
          ✓ {t('Location selected', 'স্থান নির্বাচন করা হয়েছে')}
        </p>
      )}
    </div>
  );
}

export default function FareEstimator({
  offline = false
}: {
  offline?: boolean;
}) {

  const { t, language } = useLanguage();

  const [pickup, setPickup] = useState<FareLocation | null>(null);
  const [destination, setDestination] =
    useState<FareLocation | null>(null);

  const [fare, setFare] = useState<FareResponse | null>(null);
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState(false);

  const locale = language === 'bn' ? 'bn-BD' : 'en-BD';

  function money(amount: number) {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: 'BDT'
    }).format(amount);
  }

  function changePickup(location: FareLocation | null) {
    setPickup(location);
    setFare(null);
    setError(false);
  }

  function changeDestination(location: FareLocation | null) {
    setDestination(location);
    setFare(null);
    setError(false);
  }

  async function calculateFare() {

    if (!pickup || !destination || offline || calculating) return;

    setCalculating(true);
    setFare(null);
    setError(false);

    try {
      const result = await fareApi.estimateFare({
        pickup: {
          latitude: pickup.latitude,
          longitude: pickup.longitude
        },
        destination: {
          latitude: destination.latitude,
          longitude: destination.longitude
        }
      });

      setFare(result);
    } catch {
      setError(true);
    } finally {
      setCalculating(false);
    }
  }

  return (
    <section className="am-panel fare-panel">

      <h3>
        {t('Estimate ambulance fare', 'অ্যাম্বুলেন্স ভাড়া নির্ধারণ')}
      </h3>

      <p className="am-muted">
        {t(
          'Know your estimated fare before booking.',
          'বুকিং করার আগে সম্ভাব্য ভাড়া জেনে নিন।'
        )}
      </p>

      {offline && (
        <p className="auth-alert" role="status">
          {t(
            'An internet connection is required.',
            'ইন্টারনেট সংযোগ প্রয়োজন।'
          )}
        </p>
      )}

      <div className="am-form-grid">

        <LocationPicker
          label={t('Pickup location', 'যাত্রার শুরুর স্থান')}
          selected={pickup}
          onSelect={changePickup}
          disabled={offline || calculating}
          t={t}
        />

        <LocationPicker
          label={t('Destination', 'গন্তব্য')}
          selected={destination}
          onSelect={changeDestination}
          disabled={offline || calculating}
          t={t}
        />

      </div>

      <button
        type="button"
        className="am-primary fare-calculate"
        disabled={
          !pickup || !destination || calculating || offline
        }
        onClick={() => void calculateFare()}
      >
        {calculating
          ? t('Calculating...', 'হিসাব করা হচ্ছে...')
          : t('Calculate fare', 'ভাড়া হিসাব করুন')}
      </button>

      {error && (
        <p className="auth-alert" role="alert">
          {t(
            'Unable to calculate fare. Please try again.',
            'ভাড়া হিসাব করা যায়নি। আবার চেষ্টা করুন।'
          )}
        </p>
      )}

      {fare && (
        <div className="fare-breakdown" aria-live="polite">

          <h4>{t('Fare breakdown', 'ভাড়ার বিস্তারিত')}</h4>

          <dl>
            <div>
              <dt>{t('Road distance', 'সড়কপথের দূরত্ব')}</dt>
              <dd>
                {fare.distanceKm.toLocaleString(locale)} {t('km', 'কিমি')}
              </dd>
            </div>

            <div>
              <dt>{t('Base fare', 'প্রাথমিক ভাড়া')}</dt>
              <dd>{money(fare.baseFare)}</dd>
            </div>

            <div>
              <dt>{t('Rate per km', 'প্রতি কিমি ভাড়া')}</dt>
              <dd>{money(fare.ratePerKm)}</dd>
            </div>

            <div>
              <dt>{t('Distance charge', 'দূরত্বের ভাড়া')}</dt>
              <dd>{money(fare.distanceCharge)}</dd>
            </div>

            <div className="fare-total">
              <dt>{t('Estimated total', 'মোট সম্ভাব্য ভাড়া')}</dt>
              <dd>{money(fare.totalFare)}</dd>
            </div>
          </dl>

          <p className="am-muted">
            {t(
              'This is an estimate, not a confirmed booking.',
              'এটি সম্ভাব্য ভাড়া, নিশ্চিত বুকিং নয়।'
            )}
          </p>
        </div>
      )}

      <p className="fare-attribution">
        © <a href="https://openrouteservice.org/">openrouteservice.org</a>
        {' by HeiGIT | Map data © '}
        <a href="https://www.openstreetmap.org/copyright">
          OpenStreetMap contributors
        </a>
      </p>

    </section>
  );
}