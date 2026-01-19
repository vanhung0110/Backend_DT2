-- Deduplicate any duplicate invites (keep earliest invite id)
DELETE FROM rt_invites WHERE id NOT IN (SELECT MIN(id) FROM rt_invites GROUP BY room_id, invited_user_id);
-- Add unique constraint on (room_id, invited_user_id)
ALTER TABLE rt_invites ADD CONSTRAINT uq_rt_invites_room_user UNIQUE (room_id, invited_user_id);
