CREATE TABLE communications
(
  id              UUID PRIMARY KEY      DEFAULT uuidv7(),

  institution_id  UUID         NOT NULL,
  teacher_user_id UUID         NOT NULL,
  student_id      UUID         NOT NULL,
  guardian_id     UUID         NOT NULL,

  channel         VARCHAR(64)  NOT NULL,
  provider        VARCHAR(64)  NOT NULL,
  subject         VARCHAR(200) NOT NULL,
  content         TEXT         NOT NULL,
  status          VARCHAR(16)  NOT NULL,

  created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at         TIMESTAMPTZ,
  failure_code    VARCHAR(64),
  failure_reason  VARCHAR(255),

  CONSTRAINT fk_communication_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_communication_teacher
    FOREIGN KEY (teacher_user_id)
      REFERENCES users (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_communication_student
    FOREIGN KEY (student_id, institution_id)
      REFERENCES students (id, institution_id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_communication_guardian
    FOREIGN KEY (guardian_id, institution_id)
      REFERENCES guardians (id, institution_id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_communication_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED')),

  CONSTRAINT chk_communication_completion
    CHECK (
      (status = 'PENDING' AND sent_at IS NULL AND failure_code IS NULL AND failure_reason IS NULL)
      OR (status = 'SENT' AND sent_at IS NOT NULL AND failure_code IS NULL AND failure_reason IS NULL)
      OR (status = 'FAILED' AND sent_at IS NULL AND failure_code IS NOT NULL AND failure_reason IS NOT NULL)
    )
);

CREATE INDEX idx_communications_institution_created
  ON communications (institution_id, created_at DESC);

CREATE INDEX idx_communications_teacher_student
  ON communications (teacher_user_id, student_id);
