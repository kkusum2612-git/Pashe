CREATE TABLE user_medicines (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    medicine_id UUID
        REFERENCES medicine_catalog(id),

    custom_medicine_name VARCHAR(150),

    dose_quantity NUMERIC(10, 3) NOT NULL,
    dose_unit VARCHAR(30) NOT NULL,

    dose_pattern VARCHAR(50),

    meal_timing VARCHAR(20) NOT NULL DEFAULT 'ANYTIME',

    as_needed BOOLEAN NOT NULL DEFAULT FALSE,

    start_date DATE NOT NULL,
    duration_days INTEGER,

    critical BOOLEAN NOT NULL DEFAULT FALSE,
    remaining_quantity NUMERIC(12, 3),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    stop_reason VARCHAR(255),
    ended_at TIMESTAMPTZ,

    created_by UUID NOT NULL REFERENCES users(id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_medicine_source CHECK (
        (
            medicine_id IS NOT NULL
            AND custom_medicine_name IS NULL
        )
        OR
        (
            medicine_id IS NULL
            AND custom_medicine_name IS NOT NULL
            AND LENGTH(TRIM(custom_medicine_name)) > 0
        )
    ),

    CONSTRAINT chk_user_medicine_dose
        CHECK (dose_quantity > 0),

    CONSTRAINT chk_user_medicine_duration
        CHECK (duration_days IS NULL OR duration_days > 0),

    CONSTRAINT chk_user_medicine_quantity
        CHECK (
            remaining_quantity IS NULL
            OR remaining_quantity >= 0
        ),

    CONSTRAINT chk_user_medicine_timing
        CHECK (
            meal_timing IN (
                'BEFORE_MEAL',
                'AFTER_MEAL',
                'ANYTIME'
            )
        ),

    CONSTRAINT chk_user_medicine_status
        CHECK (
            status IN ('ACTIVE', 'FINISHED', 'STOPPED')
        ),

    CONSTRAINT chk_user_medicine_schedule
        CHECK (
            (as_needed = TRUE AND dose_pattern IS NULL)
            OR
            (as_needed = FALSE AND dose_pattern IS NOT NULL)
        ),

    CONSTRAINT chk_user_medicine_stop_reason
        CHECK (
            status <> 'STOPPED'
            OR (
                stop_reason IS NOT NULL
                AND LENGTH(TRIM(stop_reason)) > 0
            )
        )
);

CREATE INDEX idx_user_medicines_user_status
    ON user_medicines (user_id, status);

CREATE INDEX idx_user_medicines_catalog
    ON user_medicines (medicine_id);