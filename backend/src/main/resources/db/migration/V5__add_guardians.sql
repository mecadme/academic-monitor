CREATE TABLE guardians
(
  id               UUID PRIMARY KEY      DEFAULT uuidv7(),

  institution_id   UUID         NOT NULL,
  platform_code    VARCHAR(32)  NOT NULL,
  external_id      VARCHAR(128) NOT NULL,
  external_user_id VARCHAR(128) NOT NULL,

  display_name     VARCHAR(200) NOT NULL,
  email            VARCHAR(320),
  system_access    BOOLEAN      NOT NULL DEFAULT FALSE,

  created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_guardian_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT uq_guardian_external
    UNIQUE (institution_id, platform_code, external_id)
);


ALTER TABLE students
  ADD CONSTRAINT uq_student_id_institution
    UNIQUE (id, institution_id);


ALTER TABLE guardians
  ADD CONSTRAINT uq_guardian_id_institution
    UNIQUE (id, institution_id);


CREATE TABLE student_guardians
(
  id                       UUID PRIMARY KEY     DEFAULT uuidv7(),

  institution_id           UUID        NOT NULL,
  student_id               UUID        NOT NULL,
  guardian_id              UUID        NOT NULL,

  relationship             VARCHAR(100),
  official_legal_guardian  BOOLEAN     NOT NULL DEFAULT FALSE,
  legal_guardian           BOOLEAN     NOT NULL DEFAULT FALSE,
  economic_representative  BOOLEAN     NOT NULL DEFAULT FALSE,
  can_pick_up              BOOLEAN     NOT NULL DEFAULT FALSE,
  lives_with_student       BOOLEAN     NOT NULL DEFAULT FALSE,

  created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_student_guardian_student
    FOREIGN KEY (student_id, institution_id)
      REFERENCES students (id, institution_id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_student_guardian_guardian
    FOREIGN KEY (guardian_id, institution_id)
      REFERENCES guardians (id, institution_id)
      ON DELETE RESTRICT,

  CONSTRAINT uq_student_guardian
    UNIQUE (student_id, guardian_id)
);


CREATE INDEX idx_guardians_institution
  ON guardians (institution_id);


CREATE INDEX idx_student_guardians_student
  ON student_guardians (student_id);
