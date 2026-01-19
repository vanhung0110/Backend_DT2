-- V4: add audio support to messages
ALTER TABLE messages
  MODIFY COLUMN content TEXT NULL,
  ADD COLUMN `type` VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  ADD COLUMN audio_url VARCHAR(512) NULL,
  ADD COLUMN audio_duration_ms BIGINT NULL;
