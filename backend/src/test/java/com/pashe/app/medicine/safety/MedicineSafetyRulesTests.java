
package com.pashe.app.medicine.safety;

import com.pashe.app.medicine.catalog.MedicineCatalogItem.Ingredient;
import com.pashe.app.medicine.safety.HealthPassportAllergyReader.Allergy;
import com.pashe.app.medicine.safety.MedicineSafetyReport.Kind;
import com.pashe.app.medicine.safety.MedicineSafetyRepository.RelatedClasses;
import com.pashe.app.medicine.safety.MedicineSafetyRules.Medicine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MedicineSafetyRulesTests {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private static final Ingredient ALPHA = ingredient(UUID.randomUUID(), "Synthetic Alpha", "Demo A", 5);
    private static final Ingredient BETA = ingredient(UUID.randomUUID(), "Synthetic Beta", "Demo B", 10);

    @Test
    void findsSharedIngredientAcrossBrandsStrengthsAndCombinations() {
        var first = medicine("Brand A", TODAY, null, ALPHA);
        var combo = medicine("Combination", TODAY, null,
            ingredient(ALPHA.id(), ALPHA.name(), ALPHA.drugClass(), 20), BETA);
        var warning = MedicineSafetyRules.duplicates(first, combo, Map.of()).getFirst();

        assertEquals(Kind.DUPLICATE_INGREDIENT, warning.kind());
        assertTrue(warning.message().contains("Brand A and Combination both contain Synthetic Alpha"));
        var time = OffsetDateTime.parse("2026-11-19T10:00:00Z");
        var acknowledged = MedicineSafetyRules.duplicates(combo, first, Map.of(warning.id(), time)).getFirst();
        assertEquals(warning.id(), acknowledged.id());
        assertEquals(time, acknowledged.acknowledgedAt());
        assertTrue(MedicineSafetyRules.duplicates(
            medicine("Old course", TODAY.minusDays(2), TODAY, ALPHA), combo, Map.of()).isEmpty());
    }

    @Test
    void distinguishesClassRelatedTextAndUnavailable() {
        var combo = medicine("Combination", TODAY, null, ALPHA, BETA);
        var exact = new Allergy(UUID.randomUUID(), "demo a", null);
        var related = new Allergy(UUID.randomUUID(), "demo c", null);
        var text = new Allergy(UUID.randomUUID(), null, "Possible reaction to SYNTHETIC BETA.");
        var classes = Set.of(new RelatedClasses("demo c", "demo b"));
        var result = MedicineSafetyRules.allergies(combo, List.of(exact, related, text), classes, Map.of());
        assertEquals(List.of(Kind.EXACT_CLASS, Kind.RELATED_CLASS, Kind.POSSIBLE_TEXT),
            result.stream().map(MedicineSafetyReport.Warning::kind).toList());
        assertTrue(MedicineSafetyRules.allergies(combo, List.of(new Allergy(UUID.randomUUID(), null,
            "Synthetic Betamax")), classes, Map.of()).isEmpty());
        assertEquals(Kind.UNCHECKABLE, MedicineSafetyRules.allergies(
            medicine("Custom", TODAY, null), List.of(), classes, Map.of()).getFirst().kind());
    }

    private static Ingredient ingredient(UUID id, String name, String drugClass, int strength) {
        return new Ingredient(id, name, drugClass, BigDecimal.valueOf(strength), "mg");
    }

    private Medicine medicine(String name, LocalDate start, LocalDate end, Ingredient... ingredients) {
        return new Medicine(UUID.randomUUID(), name, start, end, List.of(ingredients));
    }
}
