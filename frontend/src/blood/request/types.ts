export type BloodGroup =
  | "A+" | "A-" | "B+" | "B-"
  | "AB+" | "AB-" | "O+" | "O-";

export type BloodRequestUrgency =
  | "NORMAL"
  | "URGENT"
  | "CRITICAL";

export type BloodRequestStatus =
  | "ACTIVE"
  | "FULFILLED"
  | "EXPIRED";

export interface BloodRequest {
  id: string;
  requesterUserId: string;
  bloodGroup: BloodGroup;
  requiredUnits: number;
  hospitalName: string;
  urgency: BloodRequestUrgency;
  expiresAt: string;
  acceptedDonorsCount: number;
  confirmedUnits: number;
  status: BloodRequestStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBloodRequest {
  bloodGroup: BloodGroup;
  requiredUnits: number;
  hospitalName: string;
  urgency: BloodRequestUrgency;
  expiresAt: string;
}