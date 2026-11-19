
package com.pashe.app.medicine.safety;

import com.pashe.app.medicine.catalog.MedicineCatalogItem.Ingredient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

@Repository
class MedicineSafetyRepository {
    private final JdbcTemplate db;

    MedicineSafetyRepository(JdbcTemplate db) {
        this.db = db;
    }

    Map<UUID, List<Ingredient>> findIngredients(UUID userId) {
        return db.query("""
            SELECT mi.medicine_id, i.id, i.name, dc.name AS drug_class, mi.strength, mi.strength_unit
            FROM medicine_catalog_ingredients mi
            JOIN medicine_ingredients i ON i.id = mi.ingredient_id
            LEFT JOIN medicine_drug_classes dc ON dc.id = i.drug_class_id
            WHERE EXISTS (SELECT 1 FROM user_medicines um
                WHERE um.user_id = ? AND um.status = 'ACTIVE' AND um.medicine_id = mi.medicine_id)
            ORDER BY i.name, i.id
            """, rs -> {
                Map<UUID, List<Ingredient>> ingredients = new HashMap<>();
                while (rs.next()) {
                    ingredients.computeIfAbsent(rs.getObject("medicine_id", UUID.class), key -> new ArrayList<>())
                        .add(new Ingredient(rs.getObject("id", UUID.class), rs.getString("name"),
                            rs.getString("drug_class"), rs.getBigDecimal("strength"), rs.getString("strength_unit")));
                }
                return ingredients;
            }, userId);
    }

    Set<RelatedClasses> findRelatedClasses() {
        return new HashSet<>(db.query("""
            SELECT allergy_class, medicine_class FROM medicine_related_drug_classes
            """, (rs, row) -> new RelatedClasses(rs.getString(1), rs.getString(2))));
    }

    Map<String, OffsetDateTime> findAcknowledgements(UUID userId) {
        return db.query("""
            SELECT warning_id, acknowledged_at FROM medicine_safety_acknowledgements WHERE user_id = ?
            """, rs -> {
                Map<String, OffsetDateTime> acknowledged = new HashMap<>();
                while (rs.next()) {
                    acknowledged.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class));
                }
                return acknowledged;
            }, userId);
    }

    Set<MedicinePair> findIntentionalPairs(UUID userId) {
        return new HashSet<>(db.query("""
            SELECT first_medicine_id, second_medicine_id FROM medicine_safety_intentional_pairs WHERE user_id = ?
            """, (rs, row) -> new MedicinePair(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)), userId));
    }

    void acknowledge(UUID userId, String warningId) {
        db.update("""
            INSERT INTO medicine_safety_acknowledgements (user_id, warning_id, acknowledged_by)
            VALUES (?, ?, ?) ON CONFLICT DO NOTHING
            """, userId, warningId, userId);
    }

    void confirmPair(UUID userId, MedicinePair pair) {
        db.update("""
            INSERT INTO medicine_safety_intentional_pairs (user_id, first_medicine_id, second_medicine_id)
            VALUES (?, ?, ?) ON CONFLICT DO NOTHING
            """, userId, pair.first(), pair.second());
    }

    record MedicinePair(UUID first, UUID second) {
        static MedicinePair of(UUID a, UUID b) {
            return a.compareTo(b) < 0 ? new MedicinePair(a, b) : new MedicinePair(b, a);
        }
    }

    record RelatedClasses(String allergyClass, String medicineClass) {}
}
