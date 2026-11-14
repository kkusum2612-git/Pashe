
import { useEffect, useState } from 'react';
import './App.css';

type ConnectionStatus = 'checking' | 'connected' | 'disconnected';

interface HealthResponse {
  status: string;
  application: string;
  message: string;
}

function App() {
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
    setRefreshKey((previous) => previous + 1);
  }

  return (
    <div className="app">
      <header className="navbar">
        <div className="brand">
          <span className="brand-icon">+</span>
          <span>Pashe</span>
        </div>

        <span className="nav-label">Care, when it matters.</span>
      </header>

      <main className="main-content">
        <section className="welcome-section">
          <span className="eyebrow">WELCOME TO PASHE</span>

          <h1>Care is better together.</h1>

          <p className="welcome-description">
            Your companion for emergency assistance,
            trusted connections, and everyday healthcare.
          </p>
        </section>

        <section className="status-card">
          <h2>System Connection</h2>

          <p className="status-description">
            Checking communication with the Pashe backend.
          </p>

          <div className={`status-box ${connectionStatus}`}>
            <span className="status-dot"></span>

            {connectionStatus === 'checking' && (
              <span>Checking backend connection...</span>
            )}

            {connectionStatus === 'connected' && (
              <span>Backend Connected Successfully!</span>
            )}

            {connectionStatus === 'disconnected' && (
              <span>Backend Connection Failed</span>
            )}
          </div>

          <button
            className="retry-button"
            onClick={retryConnection}
            disabled={connectionStatus === 'checking'}
          >
            Check Connection Again
          </button>
        </section>

        <section className="features-section">
          <h2>What Pashe Will Offer</h2>

          <div className="feature-grid">
            <div className="feature-card">
              <span className="feature-icon">🆘</span>
              <h3>Guardian SOS</h3>
              <p>Emergency support from your trusted circle.</p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">🚑</span>
              <h3>Ambulance</h3>
              <p>Find emergency transport with fair fares.</p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">🩸</span>
              <h3>Blood Network</h3>
              <p>Connect blood donors with people in need.</p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">💊</span>
              <h3>Medicine Care</h3>
              <p>Manage medicines, reminders, and requests.</p>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        Pashe • Your care companion
      </footer>
    </div>
  );
}

export default App;
