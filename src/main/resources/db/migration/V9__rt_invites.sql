-- V9: Real-time room invites
CREATE TABLE rt_invites (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  invited_user_id BIGINT NOT NULL,
  invited_by BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rt_invites_room FOREIGN KEY (room_id) REFERENCES rt_rooms(id),
  CONSTRAINT fk_rt_invites_user FOREIGN KEY (invited_user_id) REFERENCES users(id),
  CONSTRAINT fk_rt_invites_by FOREIGN KEY (invited_by) REFERENCES users(id),
  CONSTRAINT ux_rt_invite_unique UNIQUE (room_id, invited_user_id)
);
