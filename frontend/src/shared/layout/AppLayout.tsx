import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type { User } from '../../auth/api';
import { getNavigation } from './navigation';
import AppIcon from './AppIcon';
import './app-layout.css';

interface AppLayoutProps {
  user: User;
  activePage: string;
  bn: boolean;
  onToggleLanguage: () => void;
  onSignOut: () => void;
  busy?: boolean;
  offline?: boolean;
  children: ReactNode;
}

export default function AppLayout({
  user,
  activePage,
  bn,
  onToggleLanguage,
  onSignOut,
  busy = false,
  offline = false,
  children,
}: AppLayoutProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  const drawerRef = useRef<HTMLDialogElement>(null);
  const accountMenuRef = useRef<HTMLDetailsElement>(null);

  const t = (en: string, bangla: string) => bn ? bangla : en;

  const items = getNavigation(user);
  const activeItem = items.find(item => item.page === activePage);
  const initial = user.fullName.trim().slice(0, 1).toUpperCase();

  function openMenu() {
    const drawer = drawerRef.current;

    if (drawer && !drawer.open) {
      drawer.showModal();
      setMenuOpen(true);
    }
  }

  function closeMenu() {
    if (drawerRef.current?.open) {
      drawerRef.current.close();
    }

    setMenuOpen(false);
  }

  function closeAccountMenu() {
    accountMenuRef.current?.removeAttribute('open');
  }

  useEffect(() => {
    if (drawerRef.current?.open) drawerRef.current.close();

    accountMenuRef.current?.removeAttribute('open');
  }, [activePage]);

  useEffect(() => {
    if (!window.matchMedia) return;

    const query = window.matchMedia('(min-width: 1024px)');

    function handleResize() {
      if (query.matches && drawerRef.current?.open) {
        drawerRef.current.close();
      }
    }

    query.addEventListener('change', handleResize);

    return () => query.removeEventListener('change', handleResize);
  }, []);

  useEffect(() => {
    function handleOutsideClick(event: MouseEvent) {
      const menu = accountMenuRef.current;

      if (
        menu?.open &&
        event.target instanceof Node &&
        !menu.contains(event.target)
      ) {
        menu.removeAttribute('open');
      }
    }

    function handleEscape(event: KeyboardEvent) {
      const menu = accountMenuRef.current;

      if (event.key === 'Escape' && menu?.open) {
        menu.removeAttribute('open');
        menu.querySelector('summary')?.focus();
      }
    }

    document.addEventListener('click', handleOutsideClick);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('click', handleOutsideClick);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  function brand() {
    return (
      <a
        className="app-brand"
        href="#/auth/dashboard"
        onClick={closeMenu}
      >
        <span className="app-brand-mark">
          <AppIcon name="plus" size={22} />
        </span>

        <span>{t('Pashe', 'পাশে')}</span>
      </a>
    );
  }

  function navigation() {
    return (
      <>
        <div className="app-navigation">
          <p className="app-nav-heading">
            {t('WORKSPACE', 'কর্মক্ষেত্র')}
          </p>

          <nav aria-label={t('Main navigation', 'প্রধান নেভিগেশন')}>
            {items.map(item => (
              <a
                key={item.page}
                href={`#/auth/${item.page}`}
                className={`app-nav-link${
                  activePage === item.page ? ' app-nav-active' : ''
                }`}
                aria-current={
                  activePage === item.page ? 'page' : undefined
                }
                onClick={closeMenu}
              >
                <AppIcon name={item.page === 'blood-requests' ? 'donor' : item.page} />
                <span>{bn ? item.bn : item.en}</span>
              </a>
            ))}
          </nav>
        </div>

        <div className="app-sidebar-bottom">
          <button
            className="app-sign-out"
            disabled={busy || offline}
            onClick={() => {
              closeMenu();
              onSignOut();
            }}
          >
            <AppIcon name="logout" />
            {t('Sign out', 'বের হন')}
          </button>
        </div>
      </>
    );
  }

  return (
    <div className="auth-shell app-shell" lang={bn ? 'bn' : 'en'}>
      <aside className="app-sidebar">
        <div className="app-sidebar-brand">
          {brand()}
        </div>

        {navigation()}
      </aside>

      <div className="app-workspace">
        <header className="app-header">
          <div className="app-header-left">
            <div className="app-mobile-brand">
              <button
                type="button"
                className="app-icon-button app-menu-button"
                aria-label={t('Open navigation', 'মেনু খুলুন')}
                aria-expanded={menuOpen}
                aria-controls="app-mobile-drawer"
                onClick={openMenu}
              >
                <AppIcon name="menu" size={22} />
              </button>

              {brand()}
            </div>

            <div className="app-breadcrumb">
              <span>{t('Workspace', 'কর্মক্ষেত্র')}</span>
              <span aria-hidden="true">/</span>
              <strong>
                {activeItem
                  ? bn ? activeItem.bn : activeItem.en
                  : t('Account', 'অ্যাকাউন্ট')}
              </strong>
            </div>
          </div>

          <div className="app-header-actions">
            <button
              className="app-language-button"
              type="button"
              onClick={onToggleLanguage}
            >
              <AppIcon name="language" size={18} />
              <span>{bn ? 'English' : 'বাংলা'}</span>
            </button>

            <details className="app-account-menu" ref={accountMenuRef}>
              <summary aria-label={t('Account menu', 'অ্যাকাউন্ট মেনু')}>
                <span className="app-profile-avatar">
                  {initial}
                </span>

                <span className="app-account-name">
                  {user.fullName}
                </span>

                <AppIcon name="chevron" size={16} />
              </summary>

              <div className="app-account-dropdown">
                <div className="app-account-info">
                  <strong>{user.fullName}</strong>
                  <span>{user.email}</span>
                </div>

                <a
                  href="#/auth/profile"
                  onClick={closeAccountMenu}
                >
                  <AppIcon name="profile" size={18} />
                  {t('My profile', 'আমার প্রোফাইল')}
                </a>

                <button
                  disabled={busy || offline}
                  onClick={() => {
                    closeAccountMenu();
                    onSignOut();
                  }}
                >
                  <AppIcon name="logout" size={18} />
                  {t('Sign out', 'বের হন')}
                </button>
              </div>
            </details>
          </div>
        </header>

        <main className="app-main">
          <div className="app-content">
            {children}
          </div>
        </main>

        <footer className="app-footer">
          {t(
            'Pashe · Care, when it matters.',
            'পাশে · প্রয়োজনের সময় যত্ন।'
          )}
        </footer>
      </div>

      <dialog
        ref={drawerRef}
        id="app-mobile-drawer"
        className="app-drawer"
        aria-label={t('Navigation menu', 'নেভিগেশন মেনু')}
        onClose={() => setMenuOpen(false)}
        onClick={event => {
          if (event.target === event.currentTarget) closeMenu();
        }}
      >
        <div className="app-drawer-content">
          <div className="app-drawer-header">
            {brand()}

            <button
              type="button"
              className="app-icon-button"
              aria-label={t('Close navigation', 'মেনু বন্ধ করুন')}
              onClick={closeMenu}
            >
              <AppIcon name="close" />
            </button>
          </div>

          {navigation()}
        </div>
      </dialog>
    </div>
  );
}