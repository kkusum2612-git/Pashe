import type { Role, User } from '../../auth/api';

export interface NavigationItem {
  page: string;
  en: string;
  bn: string;
  role?: Role;
}

export const workspaceRoles = {
  admin: 'ADMIN',
  ambulance: 'AMBULANCE_OPERATOR',
  pharmacy: 'PHARMACY_STAFF',
} as const;

export const navigationItems: NavigationItem[] = [
  {
    page: 'dashboard',
    en: 'Dashboard',
    bn: 'ড্যাশবোর্ড',
  },
  {
    page: 'medicine',
    en: 'Medicine Care',
    bn: 'ওষুধ সেবা',
  },
  {
    page: 'donor',
    en: 'Blood Network',
    bn: 'রক্তদান নেটওয়ার্ক',
  },
  {
    page: 'blood-requests',
    en: 'Blood Requests',
    bn: 'রক্তের অনুরোধ',
  },
  {
    page: 'verified-ambulances',
    en: 'Find an Ambulance',
    bn: 'অ্যাম্বুলেন্স খুঁজুন',
  },
  {
    page: 'ambulance',
    en: 'Ambulance Workspace',
    bn: 'অ্যাম্বুলেন্স কর্মক্ষেত্র',
    role: workspaceRoles.ambulance,
  },
  {
    page: 'pharmacy',
    en: 'Pharmacy Workspace',
    bn: 'ফার্মেসি কর্মক্ষেত্র',
    role: workspaceRoles.pharmacy,
  },
  {
    page: 'admin',
    en: 'Administration',
    bn: 'প্রশাসন',
    role: workspaceRoles.admin,
  },
  {
    page: 'profile',
    en: 'My Profile',
    bn: 'আমার প্রোফাইল',
  },
];

export function getNavigation(user: User): NavigationItem[] {
  if (!user.emailVerified) return [];

  return navigationItems.filter(
    item => !item.role || user.roles.includes(item.role)
  );
}