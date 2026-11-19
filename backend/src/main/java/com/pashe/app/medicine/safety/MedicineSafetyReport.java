
package com.pashe.app.medicine.safety;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MedicineSafetyReport(boolean allergyCheckAvailable, List<Warning> warnings) {
    public enum Kind { DUPLICATE_INGREDIENT, EXACT_CLASS, RELATED_CLASS, POSSIBLE_TEXT, UNCHECKABLE }

    public record Warning(String id, Kind kind, UUID medicineId, UUID otherMedicineId, String message,
                          OffsetDateTime acknowledgedAt) {}
}
