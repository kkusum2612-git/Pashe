import { request } from '../auth/api';

import type {
  CatalogMedicine,
  UserMedicine,
  UserMedicineRequest,
  MedicinePreferences,
  ScheduledDose,
  MedicinePreview,
  MedicinePrescription,
  MedicineSafetyReport,
} from './types';

const BASE = '/medicine';

export function searchMedicines(query = '') {
  return request<CatalogMedicine[]>(
    `${BASE}/catalog?query=${encodeURIComponent(query)}`
  );
}

export function getMyMedicines() {
  return request<UserMedicine[]>(
    `${BASE}/my-medicines`
  );
}

export function addMedicine(data: UserMedicineRequest) {
  return request<UserMedicine>(
    `${BASE}/my-medicines`,
    'POST',
    data
  );
}

export function previewMedicine(data: UserMedicineRequest, currentId: string | null) {
  const path = currentId ? '/my-medicines/' + currentId : '/my-medicines';
  return request<MedicinePreview>(BASE + path + '/preview', 'POST', data);
}

export function replaceMedicine(id: string, data: UserMedicineRequest) {
  return request<UserMedicine>(BASE + '/my-medicines/' + id + '/replacement', 'POST', data);
}

export function getPrescriptions() {
  return request<MedicinePrescription[]>(BASE + '/prescriptions');
}

export function createPrescription(data: FormData) {
  return request<MedicinePrescription>(BASE + '/prescriptions', 'POST', data);
}

export function getPrescriptionImage(id: string) {
  return request<Blob>(BASE + '/prescriptions/' + id + '/image', 'GET', undefined, true, 'blob');
}

export function getMedicineSafety() {
  return request<MedicineSafetyReport>(BASE + '/safety');
}

export function acknowledgeMedicineWarning(warningId: string, intentional: boolean) {
  return request<void>(BASE + '/safety/' + warningId + '/acknowledgement', 'PUT', { intentional });
}

export function stopMedicine(id: string, reason: string) {
  return request<UserMedicine>(
    `${BASE}/my-medicines/${id}/stop`,
    'PATCH',
    { reason }
  );
}

export function getPreferences() {
  return request<MedicinePreferences>(
    `${BASE}/preferences`
  );
}

export function savePreferences(data: MedicinePreferences) {
  return request<MedicinePreferences>(
    `${BASE}/preferences`,
    'PUT',
    data
  );
}

export function getSchedule(date?: string) {
  const query = date
    ? `?date=${encodeURIComponent(date)}`
    : '';

  return request<ScheduledDose[]>(
    `${BASE}/schedule${query}`
  );
}