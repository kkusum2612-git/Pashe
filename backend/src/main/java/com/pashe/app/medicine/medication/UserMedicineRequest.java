package com.pashe.app.medicine.medication;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UserMedicineRequest(

    UUID medicineId,

    @Size(max = 150)
    String customMedicineName,

    @NotNull
    @DecimalMin("0.001")
    @Digits(integer = 7, fraction = 3)
    BigDecimal doseQuantity,

    @NotBlank
    @Size(max = 30)
    String doseUnit,

    @Size(max = 50)
    String dosePattern,

    @NotBlank
    @Pattern(regexp = "BEFORE_MEAL|AFTER_MEAL|ANYTIME")
    String mealTiming,

    boolean asNeeded,

    @NotNull
    LocalDate startDate,

    @Positive
    @Max(36500)
    Integer durationDays,

    boolean critical,

    @PositiveOrZero
    @Digits(integer = 9, fraction = 3)
    BigDecimal remainingQuantity,

    UUID prescriptionId

) {}