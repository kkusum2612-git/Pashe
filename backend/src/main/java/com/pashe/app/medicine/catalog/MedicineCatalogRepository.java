package com.pashe.app.medicine.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MedicineCatalogRepository {

    private final JdbcTemplate db;

    public MedicineCatalogRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<MedicineCatalogItem> findAllActive() {

        String sql = """
            SELECT
                m.id,
                m.brand_name,
                m.manufacturer,
                m.form,
                m.mrp,
                m.prescription_only,
                m.requires_refrigeration,
                m.do_not_substitute,
                m.controlled_medicine,
                i.id AS ingredient_id,
                i.name AS ingredient_name,
                dc.name AS drug_class,
                mi.strength,
                mi.strength_unit
            FROM medicine_catalog m
            JOIN medicine_catalog_ingredients mi
                ON mi.medicine_id = m.id
            JOIN medicine_ingredients i
                ON i.id = mi.ingredient_id
            LEFT JOIN medicine_drug_classes dc
                ON dc.id = i.drug_class_id
            WHERE m.active = TRUE
            ORDER BY m.brand_name, m.id, i.name
            """;

        return db.query(sql, rs -> {

            Map<UUID, MedicineCatalogItem> medicines =
                new LinkedHashMap<>();

            Map<UUID, List<MedicineCatalogItem.Ingredient>>
                ingredients = new HashMap<>();

            while (rs.next()) {

                UUID id = rs.getObject("id", UUID.class);

                if (!medicines.containsKey(id)) {
                    medicines.put(id, new MedicineCatalogItem(
                        id,
                        rs.getString("brand_name"),
                        rs.getString("manufacturer"),
                        rs.getString("form"),
                        rs.getBigDecimal("mrp"),
                        rs.getBoolean("prescription_only"),
                        rs.getBoolean("requires_refrigeration"),
                        rs.getBoolean("do_not_substitute"),
                        rs.getBoolean("controlled_medicine"),
                        List.of()
                    ));
                }

                ingredients
                    .computeIfAbsent(id, key -> new ArrayList<>())
                    .add(new MedicineCatalogItem.Ingredient(
                        rs.getObject("ingredient_id", UUID.class),
                        rs.getString("ingredient_name"),
                        rs.getString("drug_class"),
                        rs.getBigDecimal("strength"),
                        rs.getString("strength_unit")
                    ));
            }

            return medicines.values().stream()
                .map(m -> new MedicineCatalogItem(
                    m.id(),
                    m.brandName(),
                    m.manufacturer(),
                    m.form(),
                    m.mrp(),
                    m.prescriptionOnly(),
                    m.requiresRefrigeration(),
                    m.doNotSubstitute(),
                    m.controlledMedicine(),
                    ingredients.get(m.id())
                ))
                .toList();
        });
    }
}