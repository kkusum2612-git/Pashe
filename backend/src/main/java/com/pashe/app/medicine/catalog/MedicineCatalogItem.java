package com.pashe.app.medicine.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MedicineCatalogItem(
    UUID id,
    String brandName,
    String manufacturer,
    String form,
    BigDecimal mrp,
    boolean prescriptionOnly,
    boolean requiresRefrigeration,
    boolean doNotSubstitute,
    boolean controlledMedicine,
    List<Ingredient> ingredients
) {
    public MedicineCatalogItem {
        ingredients = List.copyOf(ingredients);
    }

    public record Ingredient(
        UUID id,
        String name,
        String drugClass,
        BigDecimal strength,
        String strengthUnit
    ) {}
}