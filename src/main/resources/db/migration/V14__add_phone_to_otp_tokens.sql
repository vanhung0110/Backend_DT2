-- Add phone support to otp_tokens table
ALTER TABLE otp_tokens
    DROP CONSTRAINT IF EXISTS otp_tokens_email_key,
    MODIFY email VARCHAR(255) NULL,
    ADD COLUMN phone VARCHAR(20) UNIQUE NULL;

-- Create index for phone lookups
CREATE INDEX idx_otp_tokens_phone ON otp_tokens(phone) WHERE phone IS NOT NULL;
