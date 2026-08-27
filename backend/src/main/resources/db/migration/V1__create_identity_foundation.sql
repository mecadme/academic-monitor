CREATE TABLE institutions
(
  id         UUID PRIMARY KEY      DEFAULT uuidv7(),
  name       VARCHAR(150) NOT NULL,
  timezone   VARCHAR(64)  NOT NULL DEFAULT 'America/Guayaquil',
  active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users
(
  id          UUID PRIMARY KEY      DEFAULT uuidv7(),
  email       VARCHAR(320) NOT NULL,
  system_role VARCHAR(32)  NOT NULL DEFAULT 'USER',
  active      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT chk_users_system_role
    CHECK (system_role IN ('USER', 'SUPER_ADMIN'))
);

CREATE UNIQUE INDEX uq_users_email_lower
  ON users (LOWER(email));

CREATE TABLE institution_memberships
(
  id               UUID PRIMARY KEY     DEFAULT uuidv7(),

  user_id          UUID        NOT NULL,
  institution_id   UUID        NOT NULL,

  institution_role VARCHAR(32) NOT NULL,
  active           BOOLEAN     NOT NULL DEFAULT TRUE,

  created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_membership_user
    FOREIGN KEY (user_id)
      REFERENCES users (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_membership_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_membership_role
    CHECK (institution_role IN ('ADMIN', 'TEACHER')),

  CONSTRAINT uq_membership_user_institution
    UNIQUE (user_id, institution_id)
);

CREATE INDEX idx_memberships_institution_id
  ON institution_memberships (institution_id);
