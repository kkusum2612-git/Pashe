export type VerificationStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED';

export interface Equipment {
  id: string;
  equipmentName: string;
  quantity: number;
}

export interface LicenseDocument {
  id: string;
  fileName: string;
  contentType: string;
  uploadedAt: string;
}

export interface VerifiedAmbulance {
  id: string;
  registrationNumber: string;
  vehicleName: string;
  vehicleType: string;
  available: boolean;
  equipment: Equipment[];
}

export interface AmbulanceProfile extends VerifiedAmbulance {
  operatorId: string;
  verificationStatus: VerificationStatus;
  reviewedBy: string | null;
  reviewedAt: string | null;
  reviewNotes: string | null;
  createdAt: string;
  updatedAt: string;
  documents: LicenseDocument[];
}

export interface RegistrationRequest {
  registrationNumber: string;
  vehicleName: string;
  vehicleType: string;
}