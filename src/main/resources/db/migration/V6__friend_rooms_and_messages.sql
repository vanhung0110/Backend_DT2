-- V6: friend_rooms and friend_messages

CREATE TABLE IF NOT EXISTS friend_rooms (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  user_a BIGINT NOT NULL,
  user_b BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_friend_pair (user_a, user_b),
  CONSTRAINT fk_fr_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friend_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  friend_room_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fm_fr FOREIGN KEY (friend_room_id) REFERENCES friend_rooms(id) ON DELETE CASCADE,
  CONSTRAINT fk_fm_msg FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);
