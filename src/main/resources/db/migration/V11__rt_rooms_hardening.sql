-- Add updated_at to rt_invites for invite lifecycle tracking
ALTER TABLE rt_invites ADD COLUMN updated_at TIMESTAMP;
-- Backfill with created_at where available
UPDATE rt_invites SET updated_at = created_at WHERE updated_at IS NULL;
-- Leave column nullable for now to avoid breaking old systems; application will populate on update
