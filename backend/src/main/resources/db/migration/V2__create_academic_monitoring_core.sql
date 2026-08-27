CREATE TABLE academic_courses
(
  id                 UUID PRIMARY KEY      DEFAULT uuidv7(),

  institution_id     UUID         NOT NULL,
  teacher_user_id    UUID         NOT NULL,

  platform_code      VARCHAR(32)  NOT NULL DEFAULT 'DEMO',
  external_id        VARCHAR(128) NOT NULL,

  name               VARCHAR(200) NOT NULL,
  subject            VARCHAR(150),

  monitoring_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
  active             BOOLEAN      NOT NULL DEFAULT TRUE,

  created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_course_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_course_teacher
    FOREIGN KEY (teacher_user_id)
      REFERENCES users (id)
      ON DELETE RESTRICT,

  CONSTRAINT uq_course_external
    UNIQUE (institution_id, platform_code, external_id)
);


CREATE TABLE students
(
  id             UUID PRIMARY KEY      DEFAULT uuidv7(),

  institution_id UUID         NOT NULL,

  platform_code  VARCHAR(32)  NOT NULL DEFAULT 'DEMO',
  external_id    VARCHAR(128) NOT NULL,

  first_name     VARCHAR(100) NOT NULL,
  last_name      VARCHAR(100) NOT NULL,

  active         BOOLEAN      NOT NULL DEFAULT TRUE,

  created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_student_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT uq_student_external
    UNIQUE (institution_id, platform_code, external_id)
);


CREATE TABLE course_enrollments
(
  id         UUID PRIMARY KEY     DEFAULT uuidv7(),

  course_id  UUID        NOT NULL,
  student_id UUID        NOT NULL,

  active     BOOLEAN     NOT NULL DEFAULT TRUE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_enrollment_course
    FOREIGN KEY (course_id)
      REFERENCES academic_courses (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_enrollment_student
    FOREIGN KEY (student_id)
      REFERENCES students (id)
      ON DELETE RESTRICT,

  CONSTRAINT uq_course_student
    UNIQUE (course_id, student_id)
);


CREATE TABLE activities
(
  id            UUID PRIMARY KEY       DEFAULT uuidv7(),

  course_id     UUID          NOT NULL,

  platform_code VARCHAR(32)   NOT NULL DEFAULT 'DEMO',
  external_id   VARCHAR(128)  NOT NULL,

  name          VARCHAR(200)  NOT NULL,

  max_score     NUMERIC(6, 2) NOT NULL DEFAULT 10.00,

  due_date      DATE,

  active        BOOLEAN       NOT NULL DEFAULT TRUE,

  created_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_activity_course
    FOREIGN KEY (course_id)
      REFERENCES academic_courses (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_activity_max_score
    CHECK (max_score > 0),

  CONSTRAINT uq_activity_external
    UNIQUE (course_id, platform_code, external_id)
);


CREATE TABLE grades
(
  id                UUID PRIMARY KEY       DEFAULT uuidv7(),

  activity_id       UUID          NOT NULL,
  student_id        UUID          NOT NULL,

  score             NUMERIC(6, 2) NOT NULL,

  source_updated_at TIMESTAMPTZ,

  created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_grade_activity
    FOREIGN KEY (activity_id)
      REFERENCES activities (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_grade_student
    FOREIGN KEY (student_id)
      REFERENCES students (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_grade_score
    CHECK (score >= 0),

  CONSTRAINT uq_grade_activity_student
    UNIQUE (activity_id, student_id)
);


CREATE TABLE alerts
(
  id             UUID PRIMARY KEY       DEFAULT uuidv7(),

  institution_id UUID          NOT NULL,
  course_id      UUID          NOT NULL,
  activity_id    UUID          NOT NULL,
  student_id     UUID          NOT NULL,

  rule_code      VARCHAR(64)   NOT NULL,

  severity       VARCHAR(32)   NOT NULL,
  status         VARCHAR(32)   NOT NULL DEFAULT 'OPEN',

  score_snapshot NUMERIC(6, 2) NOT NULL,

  created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at    TIMESTAMPTZ,

  CONSTRAINT fk_alert_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_alert_course
    FOREIGN KEY (course_id)
      REFERENCES academic_courses (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_alert_activity
    FOREIGN KEY (activity_id)
      REFERENCES activities (id)
      ON DELETE RESTRICT,

  CONSTRAINT fk_alert_student
    FOREIGN KEY (student_id)
      REFERENCES students (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_alert_severity
    CHECK (severity IN ('WARNING', 'CRITICAL')),

  CONSTRAINT chk_alert_status
    CHECK (status IN ('OPEN', 'RESOLVED'))
);


CREATE UNIQUE INDEX uq_open_alert_per_rule
  ON alerts (activity_id, student_id, rule_code)
  WHERE status = 'OPEN';


CREATE INDEX idx_courses_teacher
  ON academic_courses (teacher_user_id);


CREATE INDEX idx_courses_institution
  ON academic_courses (institution_id);


CREATE INDEX idx_students_institution
  ON students (institution_id);


CREATE INDEX idx_activities_course
  ON activities (course_id);


CREATE INDEX idx_grades_student
  ON grades (student_id);


CREATE INDEX idx_alerts_course_status
  ON alerts (course_id, status);


CREATE INDEX idx_alerts_student
  ON alerts (student_id);
