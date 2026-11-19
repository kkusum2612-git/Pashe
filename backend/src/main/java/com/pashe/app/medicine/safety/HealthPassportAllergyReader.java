
package com.pashe.app.medicine.safety;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Health Passport supplies the authenticated patient's current allergy entries. */
public interface HealthPassportAllergyReader {
    /** Empty means unavailable. A present empty list means the passport has no entries. */
    Optional<List<Allergy>> findAllergies(UUID userId);

    /** The ID identifies an allergy entry. Class names use the medicine catalog vocabulary. */
    record Allergy(UUID id, String drugClass, String freeText) {}
}
