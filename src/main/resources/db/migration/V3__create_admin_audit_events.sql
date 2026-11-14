CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    actor_subject_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_user_id UUID NOT NULL,
    assigned_role VARCHAR(20),
    previous_enabled BOOLEAN,
    new_enabled BOOLEAN,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admin_audit_action CHECK (
        action IN ('USER_PROVISIONED', 'USER_DISABLED', 'USER_ENABLED')
    ),
    CONSTRAINT ck_admin_audit_event_details CHECK (
        (action = 'USER_PROVISIONED'
            AND assigned_role IS NOT NULL
            AND assigned_role IN ('STAFF', 'ADMIN')
            AND previous_enabled IS NULL
            AND new_enabled IS NULL)
        OR
        (action IN ('USER_DISABLED', 'USER_ENABLED')
            AND assigned_role IS NULL
            AND previous_enabled IS NOT NULL
            AND new_enabled IS NOT NULL
            AND previous_enabled <> new_enabled)
    )
);

CREATE INDEX ix_admin_audit_events_occurred_at_id
    ON admin_audit_events (occurred_at DESC, id DESC);

CREATE INDEX ix_admin_audit_events_actor_subject_id
    ON admin_audit_events (actor_subject_id);
