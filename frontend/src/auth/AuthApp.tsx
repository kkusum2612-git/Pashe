import { useEffect, useRef, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { ApiError, canAccess, request, validPhone } from './api';
import type { Role, User } from './api';
import './auth.css';

type Page = 'login' | 'register' | 'verify' | 'forgot' | 'reset' | 'profile' | 'admin' | 'ambulance' | 'pharmacy';
const pages: Page[] = ['login', 'register', 'verify', 'forgot', 'reset', 'profile', 'admin', 'ambulance', 'pharmacy'];
const rolePages: Partial<Record<Page, Role>> = { admin: 'ADMIN', ambulance: 'AMBULANCE_OPERATOR', pharmacy: 'PHARMACY_STAFF' };
const titles: Record<Page, [string, string]> = {
  login: ['Welcome back', 'আবার স্বাগতম'], register: ['Create your account', 'আপনার অ্যাকাউন্ট তৈরি করুন'],
  verify: ['Verify your email', 'ইমেইল যাচাই করুন'], forgot: ['Forgot your password?', 'পাসওয়ার্ড ভুলে গেছেন?'],
  reset: ['Choose a new password', 'নতুন পাসওয়ার্ড দিন'], profile: ['Your profile', 'আপনার প্রোফাইল'],
  admin: ['Administration', 'প্রশাসন'], ambulance: ['Ambulance workspace', 'অ্যাম্বুলেন্স কর্মক্ষেত্র'], pharmacy: ['Pharmacy workspace', 'ফার্মেসি কর্মক্ষেত্র'],
};

export default function AuthApp({ route }: { route: string }) {
  const [bn, setBn] = useState(false);
  const t = (en: string, bangla: string) => bn ? bangla : en;
  const label = (en: string, bangla: string) => <>{t(en, bangla)} <small lang={bn ? 'en' : 'bn'}>{t(bangla, en)}</small></>;
  const page: Page = pages.includes(route.split('?')[0] as Page) ? route.split('?')[0] as Page : 'login';
  const token = new URLSearchParams(route.split('?')[1]).get('token') || '';
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [offline, setOffline] = useState(!navigator.onLine);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [sessionIssue, setSessionIssue] = useState(false);
  const sessionRevision = useRef(0);
  const mutationPending = useRef(false);

  useEffect(() => {
    // Remove one-use credentials from browser history immediately.
    if (window.location.hash.includes('token=')) history.replaceState(null, '', `${location.pathname}${location.search}#/auth/${page}`);
  }, [page, token]);
  useEffect(() => {
    let active = true;
    const check = () => {
      if (mutationPending.current) return;
      const revision = sessionRevision.current;
      return request<User>('/auth/session').then(value => {
      if (active && revision === sessionRevision.current) { setUser(value); setSessionIssue(false); }
    }).catch(err => {
      if (active && revision === sessionRevision.current) { setUser(null); setSessionIssue(!(err instanceof ApiError && err.status === 401)); }
    }).finally(() => { if (active) setLoading(false); });
    };
    void check();
    const expired = () => { setUser(null); setNotice('Your session has expired. Please sign in again. / সেশনের মেয়াদ শেষ। আবার প্রবেশ করুন।'); };
    const online = () => { setOffline(false); void check(); };
    const off = () => setOffline(true);
    window.addEventListener('focus', check);
    window.addEventListener('online', online);
    window.addEventListener('offline', off);
    window.addEventListener('pashe:session-expired', expired);
    return () => { active = false; window.removeEventListener('focus', check); window.removeEventListener('online', online); window.removeEventListener('offline', off); window.removeEventListener('pashe:session-expired', expired); };
  }, []);

  async function action(task: () => Promise<void>) {
    if (mutationPending.current) return;
    mutationPending.current = true;
    sessionRevision.current += 1;
    setError(''); setNotice(''); setBusy(true);
    try { await task(); }
    catch (err) {
      const status = err instanceof ApiError ? err.status : 0;
      setError(status === 401 ? t('Email or password is incorrect, or your session expired.', 'ইমেইল বা পাসওয়ার্ড ভুল, অথবা সেশনের মেয়াদ শেষ।')
        : status === 403 ? t('Access denied. Verify your email or sign in again.', 'প্রবেশাধিকার নেই। ইমেইল যাচাই করুন অথবা আবার প্রবেশ করুন।')
        : status === 400 || status === 410 || status === 422 ? t('Check your details. This link may be invalid or expired; request a new one.', 'তথ্য যাচাই করুন। লিংকটি ভুল বা মেয়াদোত্তীর্ণ হতে পারে; নতুন লিংক চান।')
        : status === 409 ? t('Unable to use these account details. Try signing in or resetting your password.', 'এই তথ্য দিয়ে অ্যাকাউন্ট তৈরি করা যায়নি। প্রবেশ করুন বা পাসওয়ার্ড পুনরায় সেট করুন।')
        : status === 429 ? t('Too many attempts. Please wait before trying again.', 'অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ পরে আবার চেষ্টা করুন।')
        : t('Unable to reach the account service. Please try again later.', 'অ্যাকাউন্ট সেবায় সংযোগ করা যাচ্ছে না। পরে আবার চেষ্টা করুন।'));
    } finally { mutationPending.current = false; sessionRevision.current += 1; setBusy(false); }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>;
    if (data.password && new TextEncoder().encode(data.password).length > 72) { setError(t('Password must be at most 72 UTF-8 bytes.', 'পাসওয়ার্ড সর্বোচ্চ ৭২ UTF-8 বাইট হতে হবে।')); return; }
    if (data.confirmPassword !== undefined && data.password !== data.confirmPassword) { setError(t('Passwords do not match.', 'পাসওয়ার্ড দুটি মিলছে না।')); return; }
    if (data.phone && !validPhone(data.phone)) { setError(t('Enter a valid Bangladesh mobile number, e.g. 01712345678.', 'সঠিক বাংলাদেশি মোবাইল নম্বর দিন, যেমন 01712345678।')); return; }
    await action(async () => {
      if (page === 'login') {
        const current = await request<User>('/auth/login', 'POST', { email: data.email.trim(), password: data.password });
        setUser(current); location.hash = current.emailVerified ? '/auth/profile' : '/auth/verify';
      } else if (page === 'register') {
        await request('/auth/register', 'POST', { fullName: data.fullName.trim(), email: data.email.trim(), phone: data.phone.replace(/[\s-]/g, ''), password: data.password });
        setEmail(data.email); location.hash = '/auth/verify';
        setNotice(t('Check your inbox for a verification link.', 'যাচাই লিংকের জন্য আপনার ইমেইল দেখুন।'));
      } else if (page === 'forgot') {
        await request('/auth/forgot-password', 'POST', { email: data.email.trim() });
        setNotice(t('If an account matches that email, a reset link will arrive shortly. Check your spam folder too.', 'এই ইমেইলে অ্যাকাউন্ট থাকলে শীঘ্রই একটি রিসেট লিংক পাবেন। স্প্যাম ফোল্ডারও দেখুন।'));
      } else if (page === 'reset') {
        await request('/auth/reset-password', 'POST', { token, password: data.password });
        setUser(null); location.hash = '/auth/login'; setNotice(t('Password updated. Sign in with your new password.', 'পাসওয়ার্ড পরিবর্তিত হয়েছে। নতুন পাসওয়ার্ড দিয়ে প্রবেশ করুন।'));
      } else if (page === 'verify') {
        await request('/auth/resend-verification', 'POST', { email: data.email.trim() });
        setNotice(t('If verification is needed, a new link will arrive shortly.', 'যাচাই প্রয়োজন হলে শীঘ্রই একটি নতুন লিংক পাবেন।'));
      } else if (page === 'profile') {
        const updated = await request<User>('/users/me', 'PATCH', { fullName: data.fullName.trim(), phone: data.phone.replace(/[\s-]/g, '') });
        setUser(updated); setNotice(t('Your profile has been saved.', 'আপনার প্রোফাইল সংরক্ষিত হয়েছে।'));
      }
    });
  }

  function field(name: string, en: string, bangla: string, type = 'text', value = '', autoComplete = '') {
    const password = type === 'password';
    return <label className="auth-field" htmlFor={name}>{label(en, bangla)}
      <input id={name} name={name} type={password && showPassword ? 'text' : type} defaultValue={value} required
        autoComplete={autoComplete} minLength={password && page !== 'login' ? 8 : name === 'fullName' ? 2 : undefined}
        maxLength={password ? 72 : name === 'fullName' ? 100 : 254} pattern={name === 'fullName' ? '.*\\S.*' : undefined}
        placeholder={type === 'email' ? 'you@example.com' : name === 'phone' ? '01712345678' : undefined} />
    </label>;
  }
  const passwordFields = (confirm = false) => <>
    {field('password', 'Password', 'পাসওয়ার্ড', 'password', '', page === 'login' ? 'current-password' : 'new-password')}
    {confirm && <><p className="auth-hint">{t('Use at least 8 characters, up to 72 UTF-8 bytes.', 'কমপক্ষে ৮টি অক্ষর, সর্বোচ্চ ৭২ UTF-8 বাইট ব্যবহার করুন।')}</p>{field('confirmPassword', 'Confirm password', 'পাসওয়ার্ড নিশ্চিত করুন', 'password', '', 'new-password')}</>}
    <label className="auth-show"><input type="checkbox" checked={showPassword} onChange={e => setShowPassword(e.target.checked)} />{t('Show password', 'পাসওয়ার্ড দেখুন')}</label>
  </>;
  const button = (en: string, bangla: string) => <button className="auth-primary" type="submit" disabled={busy || offline}>{busy ? t('Please wait…', 'অপেক্ষা করুন…') : label(en, bangla)}</button>;
  const protectedPage = page === 'profile' || !!rolePages[page];
  let content: ReactNode;
  if (loading && protectedPage) content = <p role="status">{t('Checking your session…', 'আপনার সেশন যাচাই হচ্ছে…')}</p>;
  else if (protectedPage && !user) content = <><p>{t('Sign in to continue to your account.', 'অ্যাকাউন্ট দেখতে প্রবেশ করুন।')}</p><a className="auth-primary" href="#/auth/login">{label('Sign in', 'প্রবেশ করুন')}</a></>;
  else if (protectedPage && user && !user.emailVerified) content = <><p>{t('Verify your email before continuing.', 'চালিয়ে যেতে ইমেইল যাচাই করুন।')}</p><a href="#/auth/verify">{label('Verify email', 'ইমেইল যাচাই করুন')}</a></>;
  else if (rolePages[page] && !canAccess(user, [rolePages[page]!])) content = <><p role="alert">{t('You do not have permission to view this workspace.', 'এই কর্মক্ষেত্র দেখার অনুমতি আপনার নেই।')}</p><a href="#/auth/profile">{label('Back to profile', 'প্রোফাইলে ফিরুন')}</a></>;
  else if (rolePages[page]) content = <p>{t('Your role has been verified. Workspace features are coming soon.', 'আপনার ভূমিকা যাচাই হয়েছে। কর্মক্ষেত্রের সুবিধা শীঘ্রই আসছে।')}</p>;
  else content = <form key={page + (user?.id || '')} onSubmit={submit}><fieldset disabled={busy || offline}>
    {page === 'register' && field('fullName', 'Full name', 'পুরো নাম', 'text', '', 'name')}
    {['login', 'register', 'forgot'].includes(page) && field('email', 'Email', 'ইমেইল', 'email', '', 'email')}
    {page === 'register' && field('phone', 'Phone number', 'মোবাইল নম্বর', 'tel', '', 'tel')}
    {page === 'login' && passwordFields()}
    {page === 'register' && passwordFields(true)}
    {page === 'reset' && (token ? passwordFields(true) : <p>{t('Open the reset link from your email to choose a new password.', 'নতুন পাসওয়ার্ড দিতে ইমেইলের রিসেট লিংক খুলুন।')} <a href="#/auth/forgot">{t('Request a new link', 'নতুন লিংক চান')}</a></p>)}
    {page === 'verify' && <>
      <div className="auth-mail" aria-hidden="true">✉</div>
      <p>{t('One small step to get started. Open the verification link in your email. Check your spam folder if you cannot find it.', 'শুরু করতে আর একটি ধাপ। ইমেইলের যাচাই লিংক খুলুন। না পেলে স্প্যাম ফোল্ডার দেখুন।')}</p>
      {token && <button className="auth-primary" type="button" onClick={() => void action(async () => {
        await request('/auth/verify-email', 'POST', { token });
        setNotice(t('Email verified. You can now sign in.', 'ইমেইল যাচাই হয়েছে। এখন প্রবেশ করতে পারেন।')); location.hash = '/auth/login';
      })}>{label('Verify email', 'ইমেইল যাচাই করুন')}</button>}
      {field('email', 'Email for a new link', 'নতুন লিংকের জন্য ইমেইল', 'email', user?.email || email, 'email')}
    </>}
    {page === 'profile' && user && <>
      <div className="auth-avatar" aria-hidden="true">{user.fullName.slice(0, 1).toUpperCase()}</div>
      <p className="auth-email">{user.email}<span className="auth-badge">{t('Verified', 'যাচাইকৃত')}</span></p>
      {field('fullName', 'Full name', 'পুরো নাম', 'text', user.fullName, 'name')}
      {field('phone', 'Phone number', 'মোবাইল নম্বর', 'tel', user.phone, 'tel')}
      <p className="auth-hint">{t('Your email and roles are managed by the account service.', 'আপনার ইমেইল ও ভূমিকা অ্যাকাউন্ট সেবা দ্বারা পরিচালিত।')}</p>
      <div className="auth-roles">{user.roles.map(role => <span className="auth-badge" key={role}>{role}</span>)}</div>
    </>}
    {page === 'login' && button('Sign in', 'প্রবেশ করুন')}
    {page === 'register' && button('Create account', 'অ্যাকাউন্ট তৈরি করুন')}
    {page === 'forgot' && <><p className="auth-hint">{t('We will email you a link to reset your password.', 'পাসওয়ার্ড পরিবর্তনের লিংক ইমেইলে পাঠানো হবে।')}</p>{button('Send reset link', 'রিসেট লিংক পাঠান')}</>}
    {page === 'reset' && token && button('Reset password', 'পাসওয়ার্ড পরিবর্তন করুন')}
    {page === 'verify' && button('Resend verification email', 'যাচাই ইমেইল আবার পাঠান')}
    {page === 'profile' && button('Save changes', 'পরিবর্তন সংরক্ষণ করুন')}
    </fieldset></form>;

  return <div className="auth-shell" lang={bn ? 'bn' : 'en'}>
    <header className="auth-header"><a className="auth-brand" href="#/">✚ Pashe <span lang="bn">পাশে</span></a><button className="auth-language" onClick={() => setBn(!bn)}>{bn ? 'English' : 'বাংলা'}</button></header>
    <main className="auth-layout">
      <aside className="auth-intro"><span className="auth-eyebrow">{t('YOUR CARE COMPANION', 'আপনার যত্নের সঙ্গী')}</span><h1>{t('A little closer.','আরও একটু কাছে।')}<br />{t('A little more cared for.', 'আরও একটু যত্নে।')}</h1><p>{t('One account to stay connected to the people and care that matter to you.', 'প্রিয় মানুষ ও প্রয়োজনীয় যত্নের সাথে যুক্ত থাকতে একটি অ্যাকাউন্ট।')}</p><div className="auth-intro-note">✚ <span>{t('Together, we are Pashe.', 'একসাথে, আমরা পাশে।')}</span></div></aside>
      <section className="auth-card" aria-labelledby="auth-title"><a className="auth-back" href="#/">← {t('Back to home', 'হোমে ফিরুন')}</a><h2 id="auth-title">{titles[page][bn ? 1 : 0]}</h2><p className="auth-subtitle" lang={bn ? 'en' : 'bn'}>{titles[page][bn ? 0 : 1]}</p>
        {offline && <p className="auth-alert" role="status">{t('You are offline. Connect to manage your account.', 'আপনি অফলাইনে আছেন। অ্যাকাউন্ট পরিচালনায় ইন্টারনেট প্রয়োজন।')}</p>}
        {sessionIssue && <p className="auth-alert">{t('Account service is currently unavailable.', 'অ্যাকাউন্ট সেবা এখন চালু নেই।')}</p>}
        {error && <p className="auth-alert" role="alert">{error}</p>}{notice && <p className="auth-success" role="status">{notice}</p>}
        {content}
        <nav className="auth-links" aria-label={t('Account navigation', 'অ্যাকাউন্ট নেভিগেশন')}>
          {page === 'login' && <><a href="#/auth/forgot">{label('Forgot password?', 'পাসওয়ার্ড ভুলে গেছেন?')}</a><a href="#/auth/register">{label('Create an account', 'অ্যাকাউন্ট তৈরি করুন')}</a></>}
          {page === 'login' && <a href="#/auth/verify">{label('Resend verification email', 'যাচাই ইমেইল আবার পাঠান')}</a>}
          {!['login', 'profile'].includes(page) && <a href="#/auth/login">{label('Back to sign in', 'প্রবেশ পাতায় ফিরুন')}</a>}
          {user && <><a href="#/auth/profile">{label('My profile', 'আমার প্রোফাইল')}</a>{Object.entries(rolePages).filter(([, role]) => canAccess(user, [role])).map(([path]) => <a key={path} href={`#/auth/${path}`}>{titles[path as Page][bn ? 1 : 0]}</a>)}
            <button disabled={busy || offline} onClick={() => void action(async () => { await request('/auth/logout', 'POST'); setUser(null); location.hash = '/auth/login'; setNotice(t('You have signed out.', 'আপনি লগআউট করেছেন।')); })}>{label('Sign out', 'বের হন')}</button></>}
        </nav>
      </section>
    </main><footer className="auth-footer">{t('Pashe · Care, when it matters.', 'পাশে · প্রয়োজনের সময় যত্ন।')}</footer>
  </div>;
}
