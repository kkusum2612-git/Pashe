import { request } from '../../auth/api';

import type {
  AmbulanceProfile,
  RegistrationRequest,
  VerifiedAmbulance
} from './types';

const profiles = '/ambulance/profiles';
const adminProfiles = '/admin/ambulance-profiles';

export const ambulanceApi = {

  getMyAmbulances: () =>
    request<AmbulanceProfile[]>(profiles),

  register: (data: RegistrationRequest) =>
    request<AmbulanceProfile>(profiles, 'POST', data),

  update: (id: string, data: RegistrationRequest) =>
    request<AmbulanceProfile>(`${profiles}/${id}`, 'PUT', data),

  addEquipment: (
    id: string,
    equipmentName: string,
    quantity: number
  ) =>
    request<AmbulanceProfile>(
      `${profiles}/${id}/equipment`,
      'POST',
      { equipmentName, quantity }
    ),

  removeEquipment: (id: string, equipmentId: string) =>
    request<AmbulanceProfile>(
      `${profiles}/${id}/equipment/${equipmentId}`,
      'DELETE'
    ),

  uploadDocument: (id: string, file: File) => {
    const form = new FormData();
    form.append('file', file);

    return request<AmbulanceProfile>(
      `${profiles}/${id}/documents`,
      'POST',
      form
    );
  },

  removeDocument: (id: string, documentId: string) =>
    request<AmbulanceProfile>(
      `${profiles}/${id}/documents/${documentId}`,
      'DELETE'
    ),

  setAvailability: (id: string, available: boolean) =>
    request<AmbulanceProfile>(
      `${profiles}/${id}/availability`,
      'PATCH',
      { available }
    ),

  getPendingAmbulances: () =>
    request<AmbulanceProfile[]>(`${adminProfiles}/pending`),

  review: (
    id: string,
    verificationStatus: 'APPROVED' | 'REJECTED',
    notes: string
  ) =>
    request<AmbulanceProfile>(
      `${adminProfiles}/${id}/review`,
      'PATCH',
      { verificationStatus, notes }
    ),

  getVerifiedAmbulances: () =>
    request<VerifiedAmbulance[]>('/ambulance/verified'),

  downloadDocument: async (
    id: string,
    documentId: string,
    fileName: string,
    admin = false
  ) => {
    const prefix = admin ? adminProfiles : profiles;

    const blob = await request<Blob>(
      `${prefix}/${id}/documents/${documentId}`,
      'GET',
      undefined,
      true,
      'blob'
    );

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = fileName;

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
};