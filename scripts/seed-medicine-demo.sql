BEGIN;

INSERT INTO medicine_drug_classes (id, name)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Demo Class A'),
    ('10000000-0000-0000-0000-000000000002', 'Demo Class B')
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicine_ingredients (id, name, drug_class_id)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'Demo Ingredient A',
        '10000000-0000-0000-0000-000000000001'
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'Demo Ingredient B',
        '10000000-0000-0000-0000-000000000002'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicine_catalog
    (id, brand_name, manufacturer, form, mrp)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        'Demo Alpha', 'Demo Pharma', 'tablet', 10.00
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        'Demo Beta', 'Demo Pharma', 'tablet', 12.00
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        'Demo Plus', 'Demo Pharma', 'tablet', 15.00
    ),
    (
        '30000000-0000-0000-0000-000000000004',
        'Demo Gamma', 'Demo Pharma', 'tablet', 20.00
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicine_catalog_ingredients
    (medicine_id, ingredient_id, strength, strength_unit)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        500, 'mg'
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000001',
        500, 'mg'
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000001',
        500, 'mg'
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000002',
        50, 'mg'
    ),
    (
        '30000000-0000-0000-0000-000000000004',
        '20000000-0000-0000-0000-000000000002',
        100, 'mg'
    )
ON CONFLICT (medicine_id, ingredient_id) DO NOTHING;

COMMIT;