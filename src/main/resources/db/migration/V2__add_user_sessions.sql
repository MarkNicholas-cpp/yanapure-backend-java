-- USER SESSIONS TABLE
CREATE TABLE IF NOT EXISTS user_sessions (
  id                UUID PRIMARY KEY,
  user_id           UUID NOT NULL,
  access_token      VARCHAR(500) NOT NULL,
  refresh_token     VARCHAR(500) NOT NULL,
  expires_at        TIMESTAMPTZ NOT NULL,
  refresh_expires_at TIMESTAMPTZ NOT NULL,
  client_ip         VARCHAR(45),
  user_agent        VARCHAR(500),
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_used_at      TIMESTAMPTZ
);

-- Indexes for user_sessions
CREATE INDEX IF NOT EXISTS idx_sessions_user ON user_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON user_sessions (access_token);
CREATE INDEX IF NOT EXISTS idx_sessions_refresh ON user_sessions (refresh_token);
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON user_sessions (expires_at);

-- Foreign key constraint (optional, for referential integrity)
-- ALTER TABLE user_sessions ADD CONSTRAINT fk_sessions_user 
--   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
