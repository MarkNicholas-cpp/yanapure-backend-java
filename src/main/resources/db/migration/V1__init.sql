-- USERS
CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  phone           VARCHAR(16)  NOT NULL UNIQUE,
  email           VARCHAR(255) UNIQUE,
  role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
  last_login_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_set_updated_at ON users;
CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- OTP CHALLENGES
CREATE TABLE IF NOT EXISTS otp_challenges (
  id             UUID PRIMARY KEY,
  phone          VARCHAR(16)  NOT NULL,
  code_hash      VARCHAR(100) NOT NULL,
  expires_at     TIMESTAMPTZ  NOT NULL,
  consumed_at    TIMESTAMPTZ,
  request_ip     VARCHAR(45),
  attempt_count  INT          NOT NULL DEFAULT 0,
  verified       BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_otp_active ON otp_challenges (phone, consumed_at, expires_at);
