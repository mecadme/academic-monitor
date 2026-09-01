CREATE TABLE academic_years
(
  id             UUID PRIMARY KEY       DEFAULT uuidv7(),

  institution_id UUID          NOT NULL,

  platform_code  VARCHAR(32)   NOT NULL,
  external_id    VARCHAR(128)  NOT NULL,

  name           VARCHAR(200)  NOT NULL,
  external_year  VARCHAR(32),
  base_score     NUMERIC(8, 2) NOT NULL,

  created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_academic_year_institution
    FOREIGN KEY (institution_id)
      REFERENCES institutions (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_academic_year_base_score
    CHECK (base_score > 0),

  CONSTRAINT uq_academic_year_external
    UNIQUE (institution_id, platform_code, external_id)
);


CREATE TABLE academic_periods
(
  id               UUID PRIMARY KEY      DEFAULT uuidv7(),

  academic_year_id UUID         NOT NULL,
  external_id      VARCHAR(128) NOT NULL,

  name             VARCHAR(200) NOT NULL,
  abbreviation     VARCHAR(64),
  period_order     INTEGER      NOT NULL,

  created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_academic_period_year
    FOREIGN KEY (academic_year_id)
      REFERENCES academic_years (id)
      ON DELETE RESTRICT,

  CONSTRAINT chk_academic_period_order
    CHECK (period_order > 0),

  CONSTRAINT uq_academic_period_external
    UNIQUE (academic_year_id, external_id),

  CONSTRAINT uq_academic_period_order
    UNIQUE (academic_year_id, period_order)
);


ALTER TABLE academic_courses
  ADD COLUMN academic_year_id UUID;


ALTER TABLE activities
  ADD COLUMN academic_period_id UUID;


ALTER TABLE academic_courses
  ADD CONSTRAINT fk_course_academic_year
    FOREIGN KEY (academic_year_id)
      REFERENCES academic_years (id)
      ON DELETE RESTRICT;


ALTER TABLE activities
  ADD CONSTRAINT fk_activity_academic_period
    FOREIGN KEY (academic_period_id)
      REFERENCES academic_periods (id)
      ON DELETE RESTRICT;


CREATE INDEX idx_courses_academic_year
  ON academic_courses (academic_year_id);


CREATE INDEX idx_activities_academic_period
  ON activities (academic_period_id);
