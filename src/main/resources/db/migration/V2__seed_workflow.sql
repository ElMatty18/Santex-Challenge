INSERT INTO workflow_state (code, name, is_initial, is_final) VALUES
    (0,   'CREATED',   TRUE,  FALSE),
    (10,  'PENDING',   FALSE, FALSE),
    (100, 'COMPLETED', FALSE, TRUE);

INSERT INTO state_transition (code, from_state, to_state, allowed_role) VALUES
    ('ASSIGN',   0,   10,  'REQUESTER'),
    ('ASSIGN',   0,   10,  'ADMIN'),
    ('REASSIGN', 10,  10,  'ADMIN'),
    ('COMPLETE', 10,  100, 'REVIEWER'),
    ('COMPLETE', 10,  100, 'ADMIN'),
    ('REOPEN',   100, 10,  'ADMIN');
