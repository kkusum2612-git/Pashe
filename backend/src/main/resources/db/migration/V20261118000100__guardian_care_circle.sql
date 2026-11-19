CREATE TABLE care_circle_memberships (
 id UUID PRIMARY KEY,
 care_recipient_id UUID NOT NULL REFERENCES users(id),
 invitee_id UUID NOT NULL REFERENCES users(id),
 inviter_id UUID NOT NULL REFERENCES users(id),
 relationship_type VARCHAR(24) NOT NULL CHECK (relationship_type IN ('GUARDIAN','TRUSTED_NEIGHBOR')),
 status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','ACTIVE','DECLINED','CANCELED','REVOKED')),
 recipient_consented_at TIMESTAMPTZ NOT NULL,
 invitee_consented_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 ended_at TIMESTAMPTZ,
 CHECK (care_recipient_id <> invitee_id),
 CHECK (inviter_id = care_recipient_id),
 CHECK (status <> 'ACTIVE' OR invitee_consented_at IS NOT NULL)
);
CREATE UNIQUE INDEX circle_current_pair ON care_circle_memberships(care_recipient_id,invitee_id) WHERE status IN ('PENDING','ACTIVE');
CREATE INDEX circle_invitee ON care_circle_memberships(invitee_id,status);
CREATE TABLE care_circle_events (
 id UUID PRIMARY KEY,
 membership_id UUID NOT NULL REFERENCES care_circle_memberships(id),
 actor_id UUID NOT NULL REFERENCES users(id),
 action VARCHAR(20) NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX circle_event_membership ON care_circle_events(membership_id,occurred_at);
