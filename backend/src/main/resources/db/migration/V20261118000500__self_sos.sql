CREATE TABLE self_sos_incidents (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id),
    opened_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    pickup_address VARCHAR(300),
    resolution_reason VARCHAR(500),
    CHECK ((resolved_at IS NULL) = (resolution_reason IS NULL))
);
CREATE UNIQUE INDEX self_sos_one_active ON self_sos_incidents(recipient_id) WHERE resolved_at IS NULL;
CREATE TABLE self_sos_requests (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id),
    idempotency_key UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('COUNTDOWN','CANCELED','ACTIVATED')),
    created_at TIMESTAMPTZ NOT NULL,
    activation_deadline TIMESTAMPTZ NOT NULL,
    canceled_at TIMESTAMPTZ,
    incident_id UUID REFERENCES self_sos_incidents(id),
    pickup_address VARCHAR(300),
    UNIQUE(recipient_id,idempotency_key),
    CHECK (activation_deadline > created_at),
    CHECK ((status = 'ACTIVATED') = (incident_id IS NOT NULL)),
    CHECK ((status = 'CANCELED') = (canceled_at IS NOT NULL))
);
CREATE UNIQUE INDEX self_sos_one_countdown ON self_sos_requests(recipient_id) WHERE status='COUNTDOWN';
CREATE INDEX self_sos_due ON self_sos_requests(activation_deadline) WHERE status='COUNTDOWN';
CREATE TABLE self_sos_locations (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    accuracy DOUBLE PRECISION NOT NULL CHECK (accuracy BETWEEN 0 AND 10000),
    observed_at TIMESTAMPTZ NOT NULL
);
