-- Add updated_at to rt_members for tracking
ALTER TABLE rt_members ADD COLUMN updated_at TIMESTAMP;
UPDATE rt_members SET updated_at = joined_at WHERE updated_at IS NULL;
-- Deduplicate any accidental duplicates (keep the earliest id)
DELETE FROM rt_members WHERE id NOT IN (SELECT MIN(id) FROM rt_members GROUP BY room_id, user_id);
-- Add unique constraint on (room_id, user_id)
ALTER TABLE rt_members ADD CONSTRAINT uq_rt_members_room_user UNIQUE (room_id, user_id);
