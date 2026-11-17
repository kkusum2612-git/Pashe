-- =============================================
-- FR-06: Ambulance Profiles
-- =============================================

CREATE TABLE ambulance_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    operator_id UUID NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,

    registration_number VARCHAR(50) NOT NULL UNIQUE,
    vehicle_name VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,

    is_available BOOLEAN NOT NULL DEFAULT FALSE,

    verification_status VARCHAR(20) NOT NULL
        DEFAULT 'PENDING',

    reviewed_by UUID
        REFERENCES users(id) ON DELETE SET NULL,

    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ambulance_verification_status_check
        CHECK (verification_status IN (
            'PENDING', 'APPROVED', 'REJECTED'
        )),

    CONSTRAINT ambulance_availability_check
        CHECK (
            is_available = FALSE
            OR verification_status = 'APPROVED'
        )
);

CREATE INDEX ambulance_profiles_operator_idx
    ON ambulance_profiles(operator_id);

CREATE INDEX ambulance_profiles_status_idx
    ON ambulance_profiles(verification_status);


-- =============================================
-- Ambulance equipment
-- =============================================

CREATE TABLE ambulance_equipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ambulance_id UUID NOT NULL
        REFERENCES ambulance_profiles(id) ON DELETE CASCADE,

    equipment_name VARCHAR(100) NOT NULL,

    quantity INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT ambulance_equipment_quantity_check
        CHECK (quantity > 0),

    CONSTRAINT ambulance_equipment_unique
        UNIQUE (ambulance_id, equipment_name)
);


-- =============================================
-- Ambulance license documents
-- =============================================

CREATE TABLE ambulance_license_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ambulance_id UUID NOT NULL
        REFERENCES ambulance_profiles(id) ON DELETE CASCADE,

    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_data BYTEA NOT NULL,

    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ambulance_document_size_check
        CHECK (
            octet_length(file_data) BETWEEN 1 AND 5242880
        )
);

CREATE INDEX ambulance_license_documents_ambulance_idx
    ON ambulance_license_documents(ambulance_id);