
package com.pashe.app.medicine.safety;

import com.pashe.app.medicine.catalog.MedicineCatalogItem.Ingredient;
import com.pashe.app.medicine.safety.HealthPassportAllergyReader.Allergy;
import com.pashe.app.medicine.safety.MedicineSafetyReport.Kind;
import com.pashe.app.medicine.safety.MedicineSafetyReport.Warning;
import com.pashe.app.medicine.safety.MedicineSafetyRepository.MedicinePair;
import com.pashe.app.medicine.safety.MedicineSafetyRepository.RelatedClasses;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

final class MedicineSafetyRules {
    private MedicineSafetyRules() {}

    record Medicine(UUID id, String name, LocalDate start, LocalDate endExclusive, List<Ingredient> ingredients) {
        boolean overlaps(Medicine other) {
            return (endExclusive == null || other.start.isBefore(endExclusive))
                && (other.endExclusive == null || start.isBefore(other.endExclusive));
        }
    }

    static List<Warning> duplicates(Medicine first, Medicine second, Map<String, OffsetDateTime> acknowledged) {
        if (!first.overlaps(second)) {
            return List.of();
        }
        Set<UUID> otherIngredients = new HashSet<>();
        second.ingredients().forEach(ingredient -> otherIngredients.add(ingredient.id()));
        String pairKey = first.id() == null ? "preview:" + second.id()
            : MedicinePair.of(first.id(), second.id()).toString();
        return first.ingredients().stream().filter(ingredient -> otherIngredients.contains(ingredient.id()))
            .map(ingredient -> warning(Kind.DUPLICATE_INGREDIENT, first.id(), second.id(),
                pairKey + ":" + ingredient.id(), first.name() + " and " + second.name()
                    + " both contain " + ingredient.name() + ".", acknowledged))
            .toList();
    }

    static List<Warning> allergies(Medicine medicine, List<Allergy> allergies,
            Set<RelatedClasses> related, Map<String, OffsetDateTime> acknowledged) {
        if (medicine.ingredients().isEmpty()) {
            return List.of(warning(Kind.UNCHECKABLE, medicine.id(), null, String.valueOf(medicine.id()),
                "We couldn't check this medicine for safety because its ingredients aren't in the catalog.", acknowledged));
        }
        var warnings = new ArrayList<Warning>();
        for (var ingredient : medicine.ingredients()) {
            String drugClass = normalizeClass(ingredient.drugClass());
            String ingredientName = normalize(ingredient.name());
            for (var allergy : allergies) {
                String allergyClass = normalizeClass(allergy.drugClass());
                String freeText = normalize(allergy.freeText());
                String key = medicine.id() + ":" + ingredient.id() + ":" + allergy.id() + ":" + drugClass + ":"
                    + allergyClass + ":" + freeText;
                if (!allergyClass.isEmpty() && allergyClass.equals(drugClass)) {
                    warnings.add(warning(Kind.EXACT_CLASS, medicine.id(), null, key,
                        ingredient.name() + " belongs to your recorded allergy class (" + ingredient.drugClass()
                            + ").", acknowledged));
                } else if (related.contains(new RelatedClasses(allergyClass, drugClass))) {
                    warnings.add(warning(Kind.RELATED_CLASS, medicine.id(), null, key,
                        ingredient.name() + " belongs to a class related to a recorded allergy.", acknowledged));
                } else if (!ingredientName.isEmpty() && (" " + freeText + " ").contains(" " + ingredientName + " ")) {
                    warnings.add(warning(Kind.POSSIBLE_TEXT, medicine.id(), null, key,
                        ingredient.name() + " may match a free-text allergy in your Health Passport.", acknowledged));
                }
            }
        }
        return warnings;
    }

    private static Warning warning(Kind kind, UUID medicine, UUID other, String key, String message,
                                   Map<String, OffsetDateTime> acknowledged) {
        try {
            String id = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((kind.name() + ":" + key).getBytes(StandardCharsets.UTF_8)));
            return new Warning(id, kind, medicine, other, message, acknowledged.get(id));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String normalizeClass(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC).strip().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{M}\\p{N}]+", " ").strip();
    }
}
