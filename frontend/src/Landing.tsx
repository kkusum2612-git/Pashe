import { useEffect, useState } from 'react';
import { useLanguage } from './shared/i18n/useLanguage';
import './App.css';

type ConnectionStatus = 'checking' | 'connected' | 'disconnected';

interface HealthResponse {
  status: string;
  application: string;
  message: string;
}

function Landing() {
  const { bn, t, toggleLanguage } = useLanguage();

  const [connectionStatus, setConnectionStatus] =
    useState<ConnectionStatus>('checking');

  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function checkBackend() {
      try {
        const response = await fetch('/api/v1/health', {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error('Backend connection failed');
        }

        const data: HealthResponse = await response.json();

        if (!controller.signal.aborted) {
          setConnectionStatus(
            data.status === 'ok' ? 'connected' : 'disconnected',
          );
        }
      } catch {
        if (!controller.signal.aborted) {
          setConnectionStatus('disconnected');
        }
      }
    }

    checkBackend();

    return () => controller.abort();
  }, [refreshKey]);

  function retryConnection() {
    setConnectionStatus('checking');
    setRefreshKey(previous => previous + 1);
  }

  return (
    <div className="app" lang={bn ? 'bn' : 'en'}>
      <header className="navbar">
        <div className="brand">
          <span className="brand-icon">+</span>
          <span>{t('Pashe', 'পাশে')}</span>
        </div>

        <div className="landing-nav-actions">
          <button
            type="button"
            className="landing-language"
            onClick={toggleLanguage}
            lang={bn ? 'en' : 'bn'}
          >
            {bn ? 'English' : 'বাংলা'}
          </button>

          <a href="#/auth/login">
            {t('Sign in', 'প্রবেশ করুন')}
          </a>
        </div>
      </header>

      <main className="main-content">
        <section className="welcome-section">
          <span className="eyebrow">
            {t('WELCOME TO PASHE', 'পাশে আপনাকে স্বাগতম')}
          </span>

          <h1>
            {t('Care is better together.', 'একসাথে যত্ন আরও সুন্দর।')}
          </h1>

          <p className="welcome-description">
            {t(
              'Your companion for emergency assistance, trusted connections, and everyday healthcare.',
              'জরুরি সহায়তা, বিশ্বস্ত যোগাযোগ এবং দৈনন্দিন স্বাস্থ্যসেবায় আপনার সঙ্গী।'
            )}
          </p>
        </section>

        <section className="status-card">
          <h2>{t('System Connection', 'সিস্টেম সংযোগ')}</h2>

          <p className="status-description">
            {t(
              'Checking communication with the Pashe backend.',
              'পাশে সিস্টেমের সাথে সংযোগ যাচাই করা হচ্ছে।'
            )}
          </p>

          <div
            className={`status-box ${connectionStatus}`}
            role="status"
          >
            <span className="status-dot"></span>

            {connectionStatus === 'checking' && (
              <span>
                {t('Checking backend connection...', 'সংযোগ যাচাই করা হচ্ছে...')}
              </span>
            )}

            {connectionStatus === 'connected' && (
              <span>
                {t('Backend Connected Successfully!', 'সফলভাবে সংযোগ স্থাপিত হয়েছে!')}
              </span>
            )}

            {connectionStatus === 'disconnected' && (
              <span>
                {t('Backend Connection Failed', 'সংযোগ স্থাপন করা যায়নি')}
              </span>
            )}
          </div>

          <button
            className="retry-button"
            onClick={retryConnection}
            disabled={connectionStatus === 'checking'}
          >
            {t('Check Connection Again', 'আবার সংযোগ যাচাই করুন')}
          </button>
        </section>

        <section className="features-section">
          <h2>
            {t('What Pashe Will Offer', 'পাশের সম্ভাব্য সেবাসমূহ')}
          </h2>

          <div className="feature-grid">
            <div className="feature-card">
              <span className="feature-icon">🆘</span>
              <h3>{t('Guardian SOS', 'জরুরি সহায়তা')}</h3>
              <p>
                {t(
                  'Emergency support from your trusted circle.',
                  'বিশ্বস্ত ব্যক্তিদের কাছ থেকে জরুরি সহায়তা।'
                )}
              </p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">🚑</span>
              <h3>{t('Ambulance', 'অ্যাম্বুলেন্স')}</h3>
              <p>
                {t(
                  'Find emergency transport with fair fares.',
                  'ন্যায্য ভাড়ায় জরুরি পরিবহন খুঁজুন।'
                )}
              </p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">🩸</span>
              <h3>{t('Blood Network', 'রক্তদান নেটওয়ার্ক')}</h3>
              <p>
                {t(
                  'Connect blood donors with people in need.',
                  'রক্তদাতাদের সাথে রক্তের প্রয়োজন থাকা ব্যক্তিদের সংযুক্ত করুন।'
                )}
              </p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">💊</span>
              <h3>{t('Medicine Care', 'ওষুধ সেবা')}</h3>
              <p>
                {t(
                  'Manage medicines, reminders, and requests.',
                  'ওষুধ, স্মরণবার্তা এবং অনুরোধ পরিচালনা করুন।'
                )}
              </p>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        {t('Pashe • Your care companion', 'পাশে • আপনার যত্নের সঙ্গী')}
      </footer>
    </div>
  );
}

export default Landing;
