import { useEffect, useRef, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { ApiError, canAccess, request, validPhone } from './api';
import type { Role, User } from './api';
import './auth.css';
import DonorProfilePage from '../blood/donor/DonorProfilePage';
import BloodRequestPage from '../blood/request/BloodRequestPage';
import OperatorWorkspace from '../ambulance/profile/OperatorWorkspace';
import AdminAmbulanceWorkspace from '../ambulance/profile/AdminAmbulanceWorkspace';
import VerifiedAmbulanceDirectory from '../ambulance/profile/VerifiedAmbulanceDirectory';
import '../ambulance/profile/ambulance.css';
import AppLayout from '../shared/layout/AppLayout';
import DashboardPage from '../shared/layout/DashboardPage';
import { workspaceRoles } from '../shared/layout/navigation';
import { useLanguage } from '../shared/i18n/useLanguage';
import type { LocalizedText } from '../shared/i18n/useLanguage';
import MedicineCarePage from '../medicine/MedicineCarePage';

type Page = 'login' | 'register' | 'verify' | 'forgot' | 'reset' | 'profile' | 'admin' | 'ambulance' | 'pharmacy' | 'donor' | 'blood-requests' | 'dashboard' | 'verified-ambulances' | 'medicine';

const pages: Page[] = ['login', 'register', 'verify', 'forgot', 'reset', 'profile', 'admin', 'ambulance', 'pharmacy', 'donor', 'blood-requests', 'dashboard', 'verified-ambulances', 'medicine'];
const rolePages: Partial<Record<Page, Role>> = workspaceRoles;

const titles: Record<Page, LocalizedText> = {
  login: ['Welcome back', 'আবার স্বাগতম'],
  register: ['Create your account', 'আপনার অ্যাকাউন্ট তৈরি করুন'],
  verify: ['Verify your email', 'ইমেইল যাচাই করুন'],
  forgot: ['Forgot your password?', 'পাসওয়ার্ড ভুলে গেছেন?'],
  reset: ['Choose a new password', 'নতুন পাসওয়ার্ড দিন'],
  profile: ['Your profile', 'আপনার প্রোফাইল'],
  admin: ['Administration', 'প্রশাসন'],
  ambulance: ['Ambulance workspace', 'অ্যাম্বুলেন্স কর্মক্ষেত্র'],
  pharmacy: ['Pharmacy workspace', 'ফার্মেসি কর্মক্ষেত্র'],
  donor: ['Donor profile', 'রক্তদাতার প্রোফাইল'],
  'blood-requests': ['Blood requests', '\u09b0\u0995\u09cd\u09a4\u09c7\u09b0 \u0985\u09a8\u09c1\u09b0\u09cb\u09a7'],
  dashboard: ['Dashboard', 'ড্যাশবোর্ড'],
  'verified-ambulances': ['Verified ambulances', 'যাচাইকৃত অ্যাম্বুলেন্স'],
  medicine: ['Medicine Care', 'ওষুধ সেবা'],
};

const roleTitles: Record<Role, LocalizedText> = {
  USER: ['Personal account', 'ব্যক্তিগত অ্যাকাউন্ট'],
  ADMIN: ['Administrator', 'প্রশাসক'],
  AMBULANCE_OPERATOR: ['Ambulance operator', 'অ্যাম্বুলেন্স অপারেটর'],
  PHARMACY_STAFF: ['Pharmacy staff', 'ফার্মেসি কর্মী'],
};

export default function AuthApp({ route }: { route: string }) {
  const { bn, t, toggleLanguage } = useLanguage();

  const page: Page = pages.includes(route.split('?')[0] as Page)
    ? route.split('?')[0] as Page
    : 'login';

  const token = new URLSearchParams(route.split('?')[1]).get('token') || '';

  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<LocalizedText | null>(null);
  const [notice, setNotice] = useState<LocalizedText | null>(null);
  const [offline, setOffline] = useState(!navigator.onLine);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [sessionIssue, setSessionIssue] = useState(false);
  const sessionRevision = useRef(0);
  const mutationPending = useRef(false);

  useEffect(() => {
    // Remove one-use credentials from browser history immediately.
    if (window.location.hash.includes('token=')) {
      history.replaceState(null, '', `${location.pathname}${location.search}#/auth/${page}`);
    }
  }, [page, token]);

  useEffect(() => {
    let active = true;

    const check = () => {
      if (mutationPending.current) return;

      const revision = sessionRevision.current;

      return request<User>('/auth/session').then(value => {
        if (active && revision === sessionRevision.current) {
          setUser(value);
          setSessionIssue(false);
        }
      }).catch(err => {
        if (active && revision === sessionRevision.current) {
          setUser(null);
          setSessionIssue(!(err instanceof ApiError && err.status === 401));
        }
      }).finally(() => {
        if (active) setLoading(false);
      });
    };

    void check();

    const expired = () => {
      setUser(null);
      setNotice([
        'Your session has expired. Please sign in again.',
        'সেশনের মেয়াদ শেষ। আবার প্রবেশ করুন।'
      ]);
    };

    const online = () => { setOffline(false); void check(); };
    const off = () => setOffline(true);

    window.addEventListener('focus', check);
    window.addEventListener('online', online);
    window.addEventListener('offline', off);
    window.addEventListener('pashe:session-expired', expired);

    return () => {
      active = false;
      window.removeEventListener('focus', check);
      window.removeEventListener('online', online);
      window.removeEventListener('offline', off);
      window.removeEventListener('pashe:session-expired', expired);
    };
  }, []);

  async function action(task: () => Promise<void>) {
    if (mutationPending.current) return;

    mutationPending.current = true;
    sessionRevision.current += 1;
    setError(null);
    setNotice(null);
    setBusy(true);

    try {
      await task();
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0;

      setError(status === 401
        ? ['Email or password is incorrect, or your session expired.', 'ইমেইল বা পাসওয়ার্ড ভুল, অথবা সেশনের মেয়াদ শেষ।']
        : status === 403
        ? ['Access denied. Verify your email or sign in again.', 'প্রবেশাধিকার নেই। ইমেইল যাচাই করুন অথবা আবার প্রবেশ করুন।']
        : status === 400 || status === 410 || status === 422
        ? ['Check your details. This link may be invalid or expired; request a new one.', 'তথ্য যাচাই করুন। লিংকটি ভুল বা মেয়াদোত্তীর্ণ হতে পারে; নতুন লিংক চান।']
        : status === 409
        ? ['Unable to use these account details. Try signing in or resetting your password.', 'এই তথ্য দিয়ে অ্যাকাউন্ট তৈরি করা যায়নি। প্রবেশ করুন বা পাসওয়ার্ড পুনরায় সেট করুন।']
        : status === 429
        ? ['Too many attempts. Please wait before trying again.', 'অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ পরে আবার চেষ্টা করুন।']
        : ['Unable to reach the account service. Please try again later.', 'অ্যাকাউন্ট সেবায় সংযোগ করা যাচ্ছে না। পরে আবার চেষ্টা করুন।']);
    } finally {
      mutationPending.current = false;
      sessionRevision.current += 1;
      setBusy(false);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const data = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>;

    if (data.password && new TextEncoder().encode(data.password).length > 72) {
      setError(['Password must be at most 72 UTF-8 bytes.', 'পাসওয়ার্ড সর্বোচ্চ ৭২ UTF-8 বাইট হতে হবে।']);
      return;
    }

    if (data.confirmPassword !== undefined && data.password !== data.confirmPassword) {
      setError(['Passwords do not match.', 'পাসওয়ার্ড দুটি মিলছে না।']);
      return;
    }

    if (data.phone && !validPhone(data.phone)) {
      setError(['Enter a valid Bangladesh mobile number, e.g. 01712345678.', 'সঠিক বাংলাদেশি মোবাইল নম্বর দিন, যেমন 01712345678।']);
      return;
    }

    await action(async () => {
      if (page === 'login') {
        const current = await request<User>('/auth/login', 'POST', {
          email: data.email.trim(),
          password: data.password
        });

        setUser(current);
        location.hash = current.emailVerified ? '/auth/profile' : '/auth/verify';
      } else if (page === 'register') {
        await request('/auth/register', 'POST', {
          fullName: data.fullName.trim(),
          email: data.email.trim(),
          phone: data.phone.replace(/[\s-]/g, ''),
          password: data.password
        });

        setEmail(data.email);
        location.hash = '/auth/verify';

        setNotice([
          'Check your inbox for a verification link.',
          'যাচাই লিংকের জন্য আপনার ইমেইল দেখুন।'
        ]);
      } else if (page === 'forgot') {
        await request('/auth/forgot-password', 'POST', { email: data.email.trim() });

        setNotice([
          'If an account matches that email, a reset link will arrive shortly. Check your spam folder too.',
          'এই ইমেইলে অ্যাকাউন্ট থাকলে শীঘ্রই একটি রিসেট লিংক পাবেন। স্প্যাম ফোল্ডারও দেখুন।'
        ]);
      } else if (page === 'reset') {
        await request('/auth/reset-password', 'POST', { token, password: data.password });

        setUser(null);
        location.hash = '/auth/login';

        setNotice([
          'Password updated. Sign in with your new password.',
          'পাসওয়ার্ড পরিবর্তিত হয়েছে। নতুন পাসওয়ার্ড দিয়ে প্রবেশ করুন।'
        ]);
      } else if (page === 'verify') {
        await request('/auth/resend-verification', 'POST', { email: data.email.trim() });

        setNotice([
          'If verification is needed, a new link will arrive shortly.',
          'যাচাই প্রয়োজন হলে শীঘ্রই একটি নতুন লিংক পাবেন।'
        ]);
      } else if (page === 'profile') {
        const updated = await request<User>('/users/me', 'PATCH', {
          fullName: data.fullName.trim(),
          phone: data.phone.replace(/[\s-]/g, '')
        });

        setUser(updated);
        setNotice(['Your profile has been saved.', 'আপনার প্রোফাইল সংরক্ষিত হয়েছে।']);
      }
    });
  }

  function field(name: string, en: string, bangla: string, type = 'text', value = '', autoComplete = '') {
    const password = type === 'password';

    return <label className="auth-field" htmlFor={name}>
      {t(en, bangla)}
      <input
        id={name}
        name={name}
        type={password && showPassword ? 'text' : type}
        defaultValue={value}
        required
        autoComplete={autoComplete}
        minLength={password && page !== 'login' ? 8 : name === 'fullName' ? 2 : undefined}
        maxLength={password ? 72 : name === 'fullName' ? 100 : 254}
        pattern={name === 'fullName' ? '.*\\S.*' : undefined}
        placeholder={type === 'email' ? 'you@example.com' : name === 'phone' ? '01712345678' : undefined}
      />
    </label>;
  }

  const passwordFields = (confirm = false) => <>
    {field('password', 'Password', 'পাসওয়ার্ড', 'password', '', page === 'login' ? 'current-password' : 'new-password')}

    {confirm && <>
      <p className="auth-hint">
        {t('Use at least 8 characters, up to 72 UTF-8 bytes.', 'কমপক্ষে ৮টি অক্ষর, সর্বোচ্চ ৭২ UTF-8 বাইট ব্যবহার করুন।')}
      </p>
      {field('confirmPassword', 'Confirm password', 'পাসওয়ার্ড নিশ্চিত করুন', 'password', '', 'new-password')}
    </>}

    <label className="auth-show">
      <input type="checkbox" checked={showPassword} onChange={e => setShowPassword(e.target.checked)} />
      {t('Show password', 'পাসওয়ার্ড দেখুন')}
    </label>
  </>;

  const button = (en: string, bangla: string) =>
    <button className="auth-primary" type="submit" disabled={busy || offline}>
      {busy ? t('Please wait…', 'অপেক্ষা করুন…') : t(en, bangla)}
    </button>;

  const protectedPage =
    page === 'profile' ||
    page === 'donor' ||
    page === 'blood-requests' ||
    page === 'dashboard' ||
    page === 'verified-ambulances' ||
    page === 'medicine' ||
    !!rolePages[page];

  let content: ReactNode;

  if (loading && protectedPage) {
    content = <p role="status">{t('Checking your session…', 'আপনার সেশন যাচাই হচ্ছে…')}</p>;
  } else if (protectedPage && !user) {
    content = <>
      <p>{t('Sign in to continue to your account.', 'অ্যাকাউন্ট দেখতে প্রবেশ করুন।')}</p>
      <a className="auth-primary" href="#/auth/login">{t('Sign in', 'প্রবেশ করুন')}</a>
    </>;
  } else if (protectedPage && user && !user.emailVerified) {
    content = <>
      <p>{t('Verify your email before continuing.', 'চালিয়ে যেতে ইমেইল যাচাই করুন।')}</p>
      <a href="#/auth/verify">{t('Verify email', 'ইমেইল যাচাই করুন')}</a>
    </>;
  } else if (rolePages[page] && !canAccess(user, [rolePages[page]!])) {
    content = <>
      <p role="alert">{t('You do not have permission to view this workspace.', 'এই কর্মক্ষেত্র দেখার অনুমতি আপনার নেই।')}</p>
      <a href="#/auth/profile">{t('Back to profile', 'প্রোফাইলে ফিরুন')}</a>
    </>;
  } else if (page === 'ambulance' && user) {
    content = <OperatorWorkspace key={user.id} offline={offline} />;
  } else if (page === 'admin' && user) {
    content = <AdminAmbulanceWorkspace key={user.id} offline={offline} />;
  } else if (page === 'verified-ambulances' && user) {
    content = <VerifiedAmbulanceDirectory key={user.id} offline={offline} />;
  } else if (rolePages[page]) {
    content = <p>{t('Your role has been verified. Workspace features are coming soon.', 'আপনার ভূমিকা যাচাই হয়েছে। কর্মক্ষেত্রের সুবিধা শীঘ্রই আসছে।')}</p>;
  } else if (page === 'dashboard' && user) {
    content = <DashboardPage user={user} bn={bn} />;
  } else if (page === 'medicine' && user) {
    content = <MedicineCarePage key={user.id} offline={offline} />;
} else if (page === 'blood-requests' && user) {
    content = <BloodRequestPage key={user.id} bn={bn} offline={offline} />;
  } else if (page === 'donor' && user) {
    content = <DonorProfilePage key={user.id} bn={bn} offline={offline} />;
  } else {
    content = <form key={page + (user?.id || '')} onSubmit={submit}>
      <fieldset disabled={busy || offline}>
        {page === 'register' && field('fullName', 'Full name', 'পুরো নাম', 'text', '', 'name')}
        {['login', 'register', 'forgot'].includes(page) && field('email', 'Email', 'ইমেইল', 'email', '', 'email')}
        {page === 'register' && field('phone', 'Phone number', 'মোবাইল নম্বর', 'tel', '', 'tel')}

        {page === 'login' && passwordFields()}
        {page === 'register' && passwordFields(true)}

        {page === 'reset' && (
          token ? passwordFields(true) : <p>
            {t('Open the reset link from your email to choose a new password.', 'নতুন পাসওয়ার্ড দিতে ইমেইলের রিসেট লিংক খুলুন।')}
            {' '}<a href="#/auth/forgot">{t('Request a new link', 'নতুন লিংক চান')}</a>
          </p>
        )}

        {page === 'verify' && <>
          <div className="auth-mail" aria-hidden="true">✉</div>

          <p>{t('One small step to get started. Open the verification link in your email. Check your spam folder if you cannot find it.', 'শুরু করতে আর একটি ধাপ। ইমেইলের যাচাই লিংক খুলুন। না পেলে স্প্যাম ফোল্ডার দেখুন।')}</p>

          {token && <button className="auth-primary" type="button" onClick={() => void action(async () => {
            await request('/auth/verify-email', 'POST', { token });
            setNotice(['Email verified. You can now sign in.', 'ইমেইল যাচাই হয়েছে। এখন প্রবেশ করতে পারেন।']);
            location.hash = '/auth/login';
          })}>
            {t('Verify email', 'ইমেইল যাচাই করুন')}
          </button>}

          {field('email', 'Email for a new link', 'নতুন লিংকের জন্য ইমেইল', 'email', user?.email || email, 'email')}
        </>}

        {page === 'profile' && user && <>
          <div className="app-profile-summary">
            <div className="auth-avatar" aria-hidden="true">
              {user.fullName.slice(0, 1).toUpperCase()}
            </div>

            <div className="app-profile-identity">
              <h2>{user.fullName}</h2>

              <div className="app-profile-email">
                <span>{user.email}</span>

                <span className="app-verified-badge">
                  {t('Verified', 'যাচাইকৃত')}
                </span>
              </div>

              <div className="app-profile-roles">
                {user.roles.map(role => (
                  <span className="auth-badge" key={role}>
                    {t(...roleTitles[role])}
                  </span>
                ))}
              </div>
            </div>
          </div>

          <section
            className="app-profile-section"
            aria-labelledby="personal-information"
          >
            <h2 id="personal-information">
              {t('Personal information', 'ব্যক্তিগত তথ্য')}
            </h2>

            <p className="app-profile-description">
              {t(
                'Manage your name and contact information.',
                'আপনার নাম ও যোগাযোগের তথ্য পরিচালনা করুন।'
              )}
            </p>

            <div className="app-profile-fields">
              {field('fullName', 'Full name', 'পুরো নাম', 'text', user.fullName, 'name')}
              {field('phone', 'Phone number', 'মোবাইল নম্বর', 'tel', user.phone, 'tel')}
            </div>
          </section>
        </>}
        {page === 'login' && button('Sign in', 'প্রবেশ করুন')}
        {page === 'register' && button('Create account', 'অ্যাকাউন্ট তৈরি করুন')}

        {page === 'forgot' && <>
          <p className="auth-hint">{t('We will email you a link to reset your password.', 'পাসওয়ার্ড পরিবর্তনের লিংক ইমেইলে পাঠানো হবে।')}</p>
          {button('Send reset link', 'রিসেট লিংক পাঠান')}
        </>}

        {page === 'reset' && token && button('Reset password', 'পাসওয়ার্ড পরিবর্তন করুন')}
        {page === 'verify' && button('Resend verification email', 'যাচাই ইমেইল আবার পাঠান')}
        {page === 'profile' && (
          <div className="app-profile-actions">
            {button('Save changes', 'পরিবর্তন সংরক্ষণ করুন')}
          </div>
        )}
      </fieldset>
    </form>;
  }

  function signOut() {
    void action(async () => {
      await request('/auth/logout', 'POST');
      setUser(null);
      location.hash = '/auth/login';
      setNotice(['You have signed out.', 'আপনি লগআউট করেছেন।']);
    });
  }

  const feedback = <>
    {offline && <p className="auth-alert" role="status">
      {t('You are offline. Connect to manage your account.', 'আপনি অফলাইনে আছেন। অ্যাকাউন্ট পরিচালনায় ইন্টারনেট প্রয়োজন।')}
    </p>}

    {sessionIssue && <p className="auth-alert">
      {t('Account service is currently unavailable.', 'অ্যাকাউন্ট সেবা এখন চালু নেই।')}
    </p>}

    {error && <p className="auth-alert" role="alert">{t(...error)}</p>}
    {notice && <p className="auth-success" role="status">{t(...notice)}</p>}
  </>;

  // Shared layout for authenticated pages.
  if (protectedPage && user?.emailVerified && !loading) {
    return (
      <AppLayout
        user={user}
        activePage={page}
        bn={bn}
        onToggleLanguage={toggleLanguage}
        onSignOut={signOut}
        busy={busy}
        offline={offline}
      >
        <section
          className={`auth-card app-content-card${page === 'dashboard' || page === 'blood-requests' || page === 'verified-ambulances' || page === 'medicine' || !!rolePages[page] ? ' app-content-card-wide' : ''}`}
        >
          <h1 id="page-title">{t(...titles[page])}</h1>
          {feedback}
          {content}
        </section>
      </AppLayout>
    );
  }

  // Public authentication layout.
  return <div className="auth-shell" lang={bn ? 'bn' : 'en'}>
    <header className="auth-header">
      <a className="auth-brand" href="#/">
        ✚ {t('Pashe', 'পাশে')}
      </a>
      <button className="auth-language" onClick={toggleLanguage}>
        {bn ? 'English' : 'বাংলা'}
      </button>
    </header>

    <main className="auth-layout">
      <aside className="auth-intro">
        <span className="auth-eyebrow">{t('YOUR CARE COMPANION', 'আপনার যত্নের সঙ্গী')}</span>
        <h1>
          {t('A little closer.', 'আরও একটু কাছে।')}
          <br />
          {t('A little more cared for.', 'আরও একটু যত্নে।')}
        </h1>
        <p>{t('One account to stay connected to the people and care that matter to you.', 'প্রিয় মানুষ ও প্রয়োজনীয় যত্নের সাথে যুক্ত থাকতে একটি অ্যাকাউন্ট।')}</p>
        <div className="auth-intro-note">
          ✚ <span>{t('Together, we are Pashe.', 'একসাথে, আমরা পাশে।')}</span>
        </div>
      </aside>

      <section className="auth-card" aria-labelledby="auth-title">
        <a className="auth-back" href="#/">← {t('Back to home', 'হোমে ফিরুন')}</a>
        <h2 id="auth-title">{t(...titles[page])}</h2>

        {feedback}
        {content}

        <nav className="auth-links" aria-label={t('Account navigation', 'অ্যাকাউন্ট নেভিগেশন')}>
          {page === 'login' && <>
            <a href="#/auth/forgot">{t('Forgot password?', 'পাসওয়ার্ড ভুলে গেছেন?')}</a>
            <a href="#/auth/register">{t('Create an account', 'অ্যাকাউন্ট তৈরি করুন')}</a>
            <a href="#/auth/verify">{t('Resend verification email', 'যাচাই ইমেইল আবার পাঠান')}</a>
          </>}

          {!['login', 'profile'].includes(page) &&
            <a href="#/auth/login">{t('Back to sign in', 'প্রবেশ পাতায় ফিরুন')}</a>
          }

          {user?.emailVerified && <>
            <a href="#/auth/dashboard">{t('Dashboard', 'ড্যাশবোর্ড')}</a>
            <a href="#/auth/donor">{t('Blood Network', 'রক্তদান নেটওয়ার্ক')}</a>
            <a href="#/auth/verified-ambulances">{t('Verified Ambulances', 'যাচাইকৃত অ্যাম্বুলেন্স')}</a>
          </>}

          {user && <>
            <a href="#/auth/profile">{t('My profile', 'আমার প্রোফাইল')}</a>

            {Object.entries(rolePages)
              .filter(([, role]) => canAccess(user, [role]))
              .map(([path]) =>
                <a key={path} href={`#/auth/${path}`}>
                  {t(...titles[path as Page])}
                </a>
              )}

            <button disabled={busy || offline} onClick={signOut}>
              {t('Sign out', 'বের হন')}
            </button>
          </>}
        </nav>
      </section>
    </main>

    <footer className="auth-footer">
      {t('Pashe · Care, when it matters.', 'পাশে · প্রয়োজনের সময় যত্ন।')}
    </footer>
  </div>;
}
