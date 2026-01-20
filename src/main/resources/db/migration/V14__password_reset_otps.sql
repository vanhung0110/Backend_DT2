-- Create table to store password reset OTPs and reset tokens
CREATE TABLE password_reset_otps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  phone VARCHAR(32) NOT NULL,
  otp_hash VARCHAR(255) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  reset_token VARCHAR(128),
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_password_reset_phone ON password_reset_otps(phone);
CREATE INDEX idx_password_reset_token ON password_reset_otps(reset_token);