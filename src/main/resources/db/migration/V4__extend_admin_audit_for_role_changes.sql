ALTER TABLE admin_audit_events
    ADD COLUMN previous_role VARCHAR(20),
    ADD COLUMN new_role VARCHAR(20);

ALTER TABLE admin_audit_events
    DROP CONSTRAINT ck_admin_audit_action,
    DROP CONSTRAINT ck_admin_audit_event_details;

ALTER TABLE admin_audit_events
    ADD CONSTRAINT ck_admin_audit_action CHECK (
        action IN ('USER_PROVISIONED', 'USER_DISABLED', 'USER_ENABLED', 'USER_ROLE_CHANGED')
    ),
    ADD CONSTRAINT ck_admin_audit_event_details CHECK (
        (action = 'USER_PROVISIONED'
            AND assigned_role IS NOT NULL
            AND assigned_role IN ('STAFF', 'ADMIN')
            AND previous_enabled IS NULL
            AND new_enabled IS NULL
            AND previous_role IS NULL
            AND new_role IS NULL)
        OR
        (action IN ('USER_DISABLED', 'USER_ENABLED')
            AND assigned_role IS NULL
            AND previous_enabled IS NOT NULL
            AND new_enabled IS NOT NULL
            AND previous_enabled <> new_enabled
            AND previous_role IS NULL
            AND new_role IS NULL)
        OR
        (action = 'USER_ROLE_CHANGED'
            AND assigned_role IS NULL
            AND previous_enabled IS NULL
            AND new_enabled IS NULL
            AND previous_role IS NOT NULL
            AND previous_role IN ('USER', 'STAFF', 'ADMIN')
            AND new_role IS NOT NULL
            AND new_role IN ('USER', 'STAFF', 'ADMIN')
            AND previous_role <> new_role)
    );
