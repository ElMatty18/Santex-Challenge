INSERT INTO app_user (id, display_name, role) VALUES
    ('admin',  'Admin',          'ADMIN'),
    ('ana',    'Ana Requester',  'REQUESTER'),
    ('lucia',  'Lucia Reviewer', 'REVIEWER'),
    ('martin', 'Martin Reviewer','REVIEWER');

-- One example action per type, in different states. Explicit ids so the audit rows below can
-- reference them; the identity is restarted afterwards.
INSERT INTO action (id, type, title, description, state_code, requester_id, assignee_id,
                    outcome, comment, created_at, updated_at, version) VALUES
    (1, 'EXPENSE_APPROVAL', 'Team offsite dinner', 'Dinner for 8 people after the quarterly planning.',
        0,   'ana',   NULL,     NULL,   NULL,
        TIMESTAMP WITH TIME ZONE '2026-11-20 09:00:00+00', TIMESTAMP WITH TIME ZONE '2026-11-20 09:00:00+00', 0),
    (2, 'DEPLOYMENT_REVIEW', 'Deploy billing-service v2.3 to production', 'Adds retry on payment provider timeouts.',
        10,  'ana',   'lucia',  NULL,   NULL,
        TIMESTAMP WITH TIME ZONE '2026-11-21 10:00:00+00', TIMESTAMP WITH TIME ZONE '2026-11-21 10:00:00+00', 0),
    (3, 'DOCUMENT_UPLOAD', 'Upload signed vendor NDA', NULL,
        10,  'admin', 'martin', NULL,   NULL,
        TIMESTAMP WITH TIME ZONE '2026-11-22 11:00:00+00', TIMESTAMP WITH TIME ZONE '2026-11-22 11:00:00+00', 0),
    (4, 'ONBOARDING', 'Onboard new backend developer', 'Accounts, laptop and repository access.',
        100, 'ana',   'lucia',  'DONE', 'Accounts and laptop ready',
        TIMESTAMP WITH TIME ZONE '2026-11-18 08:00:00+00', TIMESTAMP WITH TIME ZONE '2026-11-19 15:30:00+00', 0);

ALTER TABLE action ALTER COLUMN id RESTART WITH 5;

-- Audit history that produced each seeded action's current state.
INSERT INTO audit_log (action_id, changed_at, user_id, field, old_value, new_value, request_id) VALUES
    -- 1: created by ana, not assigned yet
    (1, TIMESTAMP WITH TIME ZONE '2026-11-20 09:00:00+00', 'ana',   'state',    NULL,      'CREATED',   'seed'),
    -- 2: created and assigned to lucia by ana
    (2, TIMESTAMP WITH TIME ZONE '2026-11-21 10:00:00+00', 'ana',   'state',    NULL,      'CREATED',   'seed'),
    (2, TIMESTAMP WITH TIME ZONE '2026-11-21 10:00:00+00', 'ana',   'assignee', NULL,      'lucia',     'seed'),
    (2, TIMESTAMP WITH TIME ZONE '2026-11-21 10:00:00+00', 'ana',   'state',    'CREATED', 'PENDING',   'seed'),
    -- 3: created and assigned to martin by admin
    (3, TIMESTAMP WITH TIME ZONE '2026-11-22 11:00:00+00', 'admin', 'state',    NULL,      'CREATED',   'seed'),
    (3, TIMESTAMP WITH TIME ZONE '2026-11-22 11:00:00+00', 'admin', 'assignee', NULL,      'martin',    'seed'),
    (3, TIMESTAMP WITH TIME ZONE '2026-11-22 11:00:00+00', 'admin', 'state',    'CREATED', 'PENDING',   'seed'),
    -- 4: created and assigned to lucia by ana, completed by lucia
    (4, TIMESTAMP WITH TIME ZONE '2026-11-18 08:00:00+00', 'ana',   'state',    NULL,      'CREATED',   'seed'),
    (4, TIMESTAMP WITH TIME ZONE '2026-11-18 08:00:00+00', 'ana',   'assignee', NULL,      'lucia',     'seed'),
    (4, TIMESTAMP WITH TIME ZONE '2026-11-18 08:00:00+00', 'ana',   'state',    'CREATED', 'PENDING',   'seed'),
    (4, TIMESTAMP WITH TIME ZONE '2026-11-19 15:30:00+00', 'lucia', 'state',    'PENDING', 'COMPLETED', 'seed'),
    (4, TIMESTAMP WITH TIME ZONE '2026-11-19 15:30:00+00', 'lucia', 'outcome',  NULL,      'DONE',      'seed'),
    (4, TIMESTAMP WITH TIME ZONE '2026-11-19 15:30:00+00', 'lucia', 'comment',  NULL,      'Accounts and laptop ready', 'seed');
