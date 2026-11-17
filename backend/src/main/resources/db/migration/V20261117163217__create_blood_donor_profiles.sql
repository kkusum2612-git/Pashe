CREATE TABLE blood_donor_profiles (
    user_id UUID PRIMARY KEY
        REFERENCES users(id) ON DELETE CASCADE,

    blood_group VARCHAR(3) NOT NULL,
    area VARCHAR(120) NOT NULL,
    last_donation_date DATE,
    available BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_blood_group CHECK (
        blood_group IN (
            'A+', 'A-', 'B+', 'B-',
            'AB+', 'AB-', 'O+', 'O-'
        )
    ),

    CONSTRAINT donor_area_not_blank CHECK (
        length(btrim(area)) > 0
    )
);