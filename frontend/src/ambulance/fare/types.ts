export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface FareLocation extends Coordinates {
  label: string;
}

export interface FareEstimateRequest {
  pickup: Coordinates;
  destination: Coordinates;
}

export interface FareResponse {
  distanceKm: number;
  baseFare: number;
  ratePerKm: number;
  distanceCharge: number;
  totalFare: number;
  currency: string;
}