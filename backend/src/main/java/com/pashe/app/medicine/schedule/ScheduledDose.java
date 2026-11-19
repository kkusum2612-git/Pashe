package com.pashe.app.medicine.schedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledDose(
    String doseId,
    UUID userMedicineId,
    String medicineName,
    LocalDate scheduleDate,
    String slot,
    OffsetDateTime scheduledAt,
    BigDecimal quantity,
    String unit,
    String mealTiming,
    boolean critical,
    boolean fastingConflict
) {}