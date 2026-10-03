CREATE TABLE app_notifications
(
  id                 UUID PRIMARY KEY     DEFAULT uuidv7(),
  institution_id     UUID        NOT NULL,
  teacher_user_id    UUID        NOT NULL,
  type               VARCHAR(48) NOT NULL,
  title              VARCHAR(160) NOT NULL,
  message            VARCHAR(500) NOT NULL,
  reference_type     VARCHAR(32) NOT NULL,
  reference_id       UUID,
  academic_period_id UUID,
  event_key          VARCHAR(160),
  created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at            TIMESTAMPTZ,

  CONSTRAINT fk_app_notification_institution
    FOREIGN KEY (institution_id) REFERENCES institutions (id) ON DELETE RESTRICT,
  CONSTRAINT fk_app_notification_teacher
    FOREIGN KEY (teacher_user_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_app_notification_period
    FOREIGN KEY (academic_period_id) REFERENCES academic_periods (id) ON DELETE RESTRICT,
  CONSTRAINT chk_app_notification_type
    CHECK (type IN ('NEW_CRITICAL_ALERT', 'COMMUNICATION_SENT', 'COMMUNICATION_FAILED', 'SYNC_COMPLETED', 'SYNC_FAILED')),
  CONSTRAINT chk_app_notification_reference_type
    CHECK (reference_type IN ('ALERT', 'COMMUNICATION', 'SYNC', 'NONE')),
  CONSTRAINT chk_app_notification_reference
    CHECK ((reference_type IN ('ALERT', 'COMMUNICATION')) = (reference_id IS NOT NULL))
);

CREATE INDEX idx_app_notifications_owner_created
  ON app_notifications (institution_id, teacher_user_id, created_at DESC);

CREATE INDEX idx_app_notifications_owner_read
  ON app_notifications (institution_id, teacher_user_id, read_at);

CREATE UNIQUE INDEX uq_app_notifications_owner_event_key
  ON app_notifications (institution_id, teacher_user_id, event_key)
  WHERE event_key IS NOT NULL;
