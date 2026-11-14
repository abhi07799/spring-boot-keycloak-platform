CREATE TABLE admin_role_change_lock (
    lock_id SMALLINT PRIMARY KEY CHECK (lock_id = 1)
);

INSERT INTO admin_role_change_lock (lock_id) VALUES (1);
