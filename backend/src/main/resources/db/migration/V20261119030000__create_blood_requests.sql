CREATE TABLE blood_requests (
    id UUID PRIMARY KEY,
    requester_user_id UUID NOT NULL
        REFERENCES users(id),

    blood_group VARCHAR(3) NOT NULL,
    required_units INTEGER NOT NULL,
    hospital_name VARCHAR(200) NOT NULL,
    urgency VARCHAR(20) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    accepted_donors_count INTEGER NOT NULL DEFAULT 0,
    confirmed_units INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT blood_request_group_valid CHECK (
        blood_group IN (
            'A+', 'A-', 'B+', 'B-',
            'AB+', 'AB-', 'O+', 'O-'
        )
    ),

    CONSTRAINT blood_request_units_positive CHECK (
        required_units > 0
    ),

    CONSTRAINT blood_request_hospital_not_blank CHECK (
        length(btrim(hospital_name)) > 0
    ),

    CONSTRAINT blood_request_urgency_valid CHECK (
        urgency IN ('NORMAL', 'URGENT', 'CRITICAL')
    ),

    CONSTRAINT blood_request_status_valid CHECK (
        status IN ('ACTIVE', 'FULFILLED', 'EXPIRED')
    ),

    CONSTRAINT blood_request_counts_nonnegative CHECK (
        accepted_donors_count >= 0
        AND confirmed_units >= 0
    ),

    CONSTRAINT blood_request_deadline_valid CHECK (
        expires_at > created_at
    )
);

CREATE INDEX idx_blood_requests_requester
    ON blood_requests(requester_user_id);

CREATE INDEX idx_blood_requests_status_expiry
    ON blood_requests(status, expires_at);
