package com.pashe.app.medicine.medication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserMedicine(
    UUID id,
    UUID medicineId,
    String medicineName,
    boolean customMedicine,
    BigDecimal doseQuantity,
    String doseUnit,
    String dosePattern,
    String mealTiming,
    boolean asNeeded,
    LocalDate startDate,
    Integer durationDays,
    boolean critical,
    BigDecimal remainingQuantity,
    String status,
    String stopReason,
    OffsetDateTime endedAt,
    UUID createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    UUID prescriptionId,
    UUID previousMedicineId,
    LocalDate scheduleEndDate
) {}