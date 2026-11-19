export interface MedicineIngredient {
  id: string;
  name: string;
  drugClass: string | null;
  strength: number;
  strengthUnit: string;
}

export interface CatalogMedicine {
  id: string;
  brandName: string;
  manufacturer: string;
  form: string;
  mrp: number;
  ingredients: MedicineIngredient[];
}

export type MealTiming =
  | 'BEFORE_MEAL'
  | 'AFTER_MEAL'
  | 'ANYTIME';

export interface UserMedicineRequest {
  medicineId: string | null;
  customMedicineName: string | null;
  doseQuantity: number;
  doseUnit: string;
  dosePattern: string | null;
  mealTiming: MealTiming;
  asNeeded: boolean;
  startDate: string;
  durationDays: number | null;
  critical: boolean;
  remainingQuantity: number | null;
  prescriptionId: string | null;
}

export interface UserMedicine {
  id: string;
  medicineId: string | null;
  medicineName: string;
  customMedicine: boolean;
  doseQuantity: number;
  doseUnit: string;
  dosePattern: string | null;
  mealTiming: MealTiming;
  asNeeded: boolean;
  startDate: string;
  durationDays: number | null;
  critical: boolean;
  remainingQuantity: number | null;
  status: 'ACTIVE' | 'FINISHED' | 'STOPPED';
  stopReason: string | null;
  prescriptionId: string | null;
  previousMedicineId: string | null;
  scheduleEndDate: string | null;
}

export interface MedicinePreferences {
  breakfastTime: string;
  lunchTime: string;
  dinnerTime: string;
  bedtime: string;
  timezone: string;
  ramadan: Ramadan | null;
}

export interface Ramadan {
  startDate: string;
  endDate: string;
  sehriTime: string;
  iftarTime: string;
}

export interface MedicinePrescription {
  id: string;
  doctorName: string;
  prescriptionDate: string;
  hasImage: boolean;
}

export interface MedicinePreview {
  medicineName: string;
  normalizedPattern: string | null;
  doseQuantity: number;
  doseUnit: string;
  mealTiming: MealTiming;
  startDate: string;
  durationDays: number | null;
  safety: MedicineSafetyReport;
  doses: (Pick<ScheduledDose, 'slot' | 'scheduledAt' | 'quantity' | 'fastingConflict'> & { meal: string })[];
}

export interface MedicineSafetyReport {
  allergyCheckAvailable: boolean;
  warnings: MedicineSafetyWarning[];
}

export interface MedicineSafetyWarning {
  id: string;
  kind: 'DUPLICATE_INGREDIENT' | 'EXACT_CLASS' | 'RELATED_CLASS' | 'POSSIBLE_TEXT' | 'UNCHECKABLE';
  medicineId: string | null;
  otherMedicineId: string | null;
  message: string;
  acknowledgedAt: string | null;
}

export interface ScheduledDose {
  doseId: string;
  userMedicineId: string;
  medicineName: string;
  scheduleDate: string;
  slot: 'MORNING' | 'NOON' | 'NIGHT' | 'BEDTIME';
  scheduledAt: string | null;
  fastingConflict: boolean;
  quantity: number;
  unit: string;
  mealTiming: MealTiming;
  critical: boolean;
}