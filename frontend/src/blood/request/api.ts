import { request } from '../../auth/api';
import type {
  BloodRequest,
  CreateBloodRequest
} from './types';

export function getBloodRequests(): Promise<BloodRequest[]> {
  return request<BloodRequest[]>('/blood/requests');
}

export function getBloodRequest(
  id: string
): Promise<BloodRequest> {
  return request<BloodRequest>(
    `/blood/requests/${encodeURIComponent(id)}`
  );
}

export function createBloodRequest(
  data: CreateBloodRequest
): Promise<BloodRequest> {
  return request<BloodRequest>(
    '/blood/requests',
    'POST',
    data
  );
}