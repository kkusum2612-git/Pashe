CREATE TABLE medicine_drug_classes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_medicine_drug_class_name UNIQUE (name)
);

CREATE TABLE medicine_ingredients (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    drug_class_id UUID REFERENCES medicine_drug_classes(id),
    CONSTRAINT uq_medicine_ingredient_name UNIQUE (name)
);

CREATE TABLE medicine_catalog (
    id UUID PRIMARY KEY,
    brand_name VARCHAR(150) NOT NULL,
    manufacturer VARCHAR(150) NOT NULL,
    form VARCHAR(50) NOT NULL,
    mrp NUMERIC(12, 2) NOT NULL,

    prescription_only BOOLEAN NOT NULL DEFAULT FALSE,
    requires_refrigeration BOOLEAN NOT NULL DEFAULT FALSE,
    do_not_substitute BOOLEAN NOT NULL DEFAULT FALSE,
    controlled_medicine BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_medicine_mrp CHECK (mrp >= 0)
);

CREATE TABLE medicine_catalog_ingredients (
    medicine_id UUID NOT NULL
        REFERENCES medicine_catalog(id) ON DELETE CASCADE,

    ingredient_id UUID NOT NULL
        REFERENCES medicine_ingredients(id),

    strength NUMERIC(12, 4) NOT NULL,
    strength_unit VARCHAR(30) NOT NULL,

    PRIMARY KEY (medicine_id, ingredient_id),

    CONSTRAINT chk_ingredient_strength CHECK (strength > 0)
);

CREATE INDEX idx_medicine_catalog_brand
    ON medicine_catalog (LOWER(brand_name));

CREATE INDEX idx_medicine_ingredient_class
    ON medicine_ingredients (drug_class_id);

CREATE INDEX idx_catalog_ingredient
    ON medicine_catalog_ingredients (ingredient_id);