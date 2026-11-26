-- Who completed an action and when. Set on COMPLETE, cleared on REOPEN. Not audited: derived
-- from the state/outcome changes, kept here so a conflicting COMPLETE can report them (409).
ALTER TABLE action ADD COLUMN completed_by VARCHAR(50);
ALTER TABLE action ADD COLUMN completed_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE action ADD CONSTRAINT fk_action_completed_by FOREIGN KEY (completed_by) REFERENCES app_user (id);

-- Seeded action 4 was completed by lucia (see V3 audit rows)
UPDATE action
SET completed_by = 'lucia',
    completed_at = TIMESTAMP WITH TIME ZONE '2026-11-19 15:30:00+00'
WHERE id = 4;
