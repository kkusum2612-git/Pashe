import { request, ApiError } from '../../auth/api';

export type BloodGroup =
  | 'A+' | 'A-'
  | 'B+' | 'B-'
  | 'AB+' | 'AB-'
  | 'O+' | 'O-';

export interface DonorProfileRequest {
  bloodGroup: BloodGroup;
  area: string;
  lastDonationDate: string | null;
  available: boolean;
}

export interface DonorProfile extends DonorProfileRequest {
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export async function getDonorProfile(): Promise<DonorProfile | null> {
  try {
    return await request<DonorProfile>('/blood/donor-profile');
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }

    throw error;
  }
}

export function saveDonorProfile(
  profile: DonorProfileRequest
): Promise<DonorProfile> {
  return request<DonorProfile>(
    '/blood/donor-profile',
    'PUT',
    profile
  );
}