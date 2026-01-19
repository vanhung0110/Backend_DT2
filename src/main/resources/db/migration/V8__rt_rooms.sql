-- V8: Real-time rooms (rt_rooms) and members (rt_members)
CREATE TABLE rt_rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  is_public BOOLEAN NOT NULL DEFAULT TRUE,
  owner_id BIGINT NOT NULL,
  description TEXT,
  max_members INT DEFAULT 50,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rt_rooms_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE rt_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'member',
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_seen TIMESTAMP NULL,
  muted BOOLEAN DEFAULT FALSE,
  volume INT DEFAULT 100,
  kicked BOOLEAN DEFAULT FALSE,
  CONSTRAINT ux_rt_room_user UNIQUE (room_id, user_id),
  CONSTRAINT fk_rt_members_room FOREIGN KEY (room_id) REFERENCES rt_rooms(id),
  CONSTRAINT fk_rt_members_user FOREIGN KEY (user_id) REFERENCES users(id)
);
