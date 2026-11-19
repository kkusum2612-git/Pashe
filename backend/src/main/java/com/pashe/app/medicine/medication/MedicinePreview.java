
package com.pashe.app.medicine.medication;

import com.pashe.app.medicine.safety.MedicineSafetyReport;
import com.pashe.app.medicine.schedule.MedicineScheduleRules.Dose;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MedicinePreview(String medicineName, String normalizedPattern, BigDecimal doseQuantity,
                              String doseUnit, String mealTiming, LocalDate startDate, Integer durationDays,
                              List<Dose> doses, MedicineSafetyReport safety) {}
