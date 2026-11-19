import { useEffect, useState } from 'react';
import Landing from './Landing';
import AuthApp from './auth/AuthApp';
import { PwaStatus } from './PwaStatus';
import GuardianApp from './features/guardian/GuardianApp';

export default function App() {
  const [hash, setHash] = useState(location.hash);
  useEffect(() => {
    const update = () => { setHash(location.hash); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);
  return <>{hash.startsWith('#/guardian') ? <GuardianApp route={hash.slice(10)} /> : hash.startsWith('#/auth/') ? <AuthApp route={hash.slice(7)} /> : <Landing />}<PwaStatus suppressed={hash.startsWith('#/guardian')} /></>;
}
