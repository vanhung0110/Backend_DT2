-- Add sleeping and last_activity_at to rt_rooms, and online to rt_members
ALTER TABLE rt_rooms ADD COLUMN sleeping boolean DEFAULT false;
ALTER TABLE rt_rooms ADD COLUMN last_activity_at timestamp;
ALTER TABLE rt_members ADD COLUMN online boolean DEFAULT false;
