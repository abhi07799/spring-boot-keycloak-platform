CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    actor_subject_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_user_id UUID NOT NULL,
    assigned_role VARCHAR(20),
    previous_enabled BOOLEAN,
    new_enabled BOOLEAN,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_admin_audit_events_occurred_at_id
    ON admin_audit_events (occurred_at DESC, id DESC);

CREATE INDEX ix_admin_audit_events_actor_subject_id
    ON admin_audit_events (actor_subject_id);
