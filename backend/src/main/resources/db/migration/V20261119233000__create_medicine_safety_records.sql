
CREATE TABLE medicine_safety_acknowledgements (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    warning_id VARCHAR(64) NOT NULL,
    acknowledged_by UUID NOT NULL REFERENCES users(id),
    acknowledged_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, warning_id)
);

CREATE TABLE medicine_safety_intentional_pairs (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_medicine_id UUID NOT NULL,
    second_medicine_id UUID NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, first_medicine_id, second_medicine_id),
    FOREIGN KEY (user_id, first_medicine_id) REFERENCES user_medicines(user_id, id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, second_medicine_id) REFERENCES user_medicines(user_id, id) ON DELETE CASCADE,
    CHECK (first_medicine_id <> second_medicine_id)
);

CREATE TABLE medicine_related_drug_classes (
    allergy_class VARCHAR(100) NOT NULL,
    medicine_class VARCHAR(100) NOT NULL,
    PRIMARY KEY (allergy_class, medicine_class),
    CHECK (allergy_class <> medicine_class)
);

INSERT INTO medicine_related_drug_classes (allergy_class, medicine_class)
VALUES ('penicillin', 'cephalosporin'), ('cephalosporin', 'penicillin');
