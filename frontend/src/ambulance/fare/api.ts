import { request } from '../../auth/api';

import type {
  FareLocation,
  FareEstimateRequest,
  FareResponse
} from './types';

const basePath = '/ambulance/fare';

export const fareApi = {
  searchLocations: (query: string) =>
    request<FareLocation[]>(
      `${basePath}/locations?query=${encodeURIComponent(query)}`
    ),

  estimateFare: (data: FareEstimateRequest) =>
    request<FareResponse>(
      `${basePath}/estimate`,
      'POST',
      data
    )
};