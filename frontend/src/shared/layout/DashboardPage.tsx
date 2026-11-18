import type { User } from '../../auth/api';
import { getNavigation } from './navigation';
import AppIcon from './AppIcon';

const descriptions: Record<string, [string, string]> = {
  donor: [
    'Manage your blood donor information and availability.',
    'রক্তদাতার তথ্য এবং রক্তদানের প্রস্তুতি পরিচালনা করুন।'
  ],
  ambulance: [
    'Manage ambulance-related services.',
    'অ্যাম্বুলেন্স সংক্রান্ত সেবা পরিচালনা করুন।'
  ],
  'verified-ambulances': [
    'Browse verified ambulance providers.',
    'যাচাইকৃত অ্যাম্বুলেন্স সেবা প্রদানকারীদের খুঁজুন।'
  ],
  pharmacy: [
    'Access your pharmacy workspace.',
    'ফার্মেসি কর্মক্ষেত্রে প্রবেশ করুন।'
  ],
  admin: [
    'Manage administrative operations.',
    'প্রশাসনিক কার্যক্রম পরিচালনা করুন।'
  ],
  profile: [
    'View and update your personal information.',
    'আপনার ব্যক্তিগত তথ্য দেখুন ও পরিবর্তন করুন।'
  ],
};

export default function DashboardPage({ user, bn }: {
  user: User;
  bn: boolean;
}) {
  const t = (en: string, bangla: string) => bn ? bangla : en;

  const items = getNavigation(user)
    .filter(item => item.page !== 'dashboard');

  return (
    <>
      <div className="app-welcome">
        <h2>
          {t('Welcome', 'স্বাগতম')}, {user.fullName}
        </h2>

        <p>
          {t(
            'Access your healthcare services from one place.',
            'এক জায়গা থেকে আপনার স্বাস্থ্যসেবা ব্যবহার করুন।'
          )}
        </p>
      </div>

      <h2 className="app-dashboard-heading">
        {t('Your services', 'আপনার সেবাসমূহ')}
      </h2>

      <div className="app-dashboard-grid">
        {items.map(item => (
          <a
            key={item.page}
            href={`#/auth/${item.page}`}
            className="app-service-card"
          >
            <span className="app-service-icon">
              <AppIcon name={item.page} size={24} />
            </span>

            <strong>{bn ? item.bn : item.en}</strong>

            <p>
              {descriptions[item.page]?.[bn ? 1 : 0] ??
                t('Open this service.', 'সেবাটি ব্যবহার করুন।')}
            </p>

            <span className="app-service-arrow">
              <AppIcon name="arrow" size={18} />
            </span>
          </a>
        ))}
      </div>
    </>
  );
}