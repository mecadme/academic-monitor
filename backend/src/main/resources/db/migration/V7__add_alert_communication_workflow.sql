ALTER TABLE communications
  ADD COLUMN alert_id UUID,
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE communications DROP CONSTRAINT chk_communication_status;
ALTER TABLE communications DROP CONSTRAINT chk_communication_completion;

ALTER TABLE communications
  ADD CONSTRAINT fk_communication_alert
    FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE RESTRICT,
  ADD CONSTRAINT chk_communication_status
    CHECK (status IN ('DRAFT', 'PENDING', 'SENT', 'FAILED')),
  ADD CONSTRAINT chk_communication_completion
    CHECK (
      (status IN ('DRAFT', 'PENDING') AND sent_at IS NULL AND failure_code IS NULL AND failure_reason IS NULL)
      OR (status = 'SENT' AND sent_at IS NOT NULL AND failure_code IS NULL AND failure_reason IS NULL)
      OR (status = 'FAILED' AND sent_at IS NULL AND failure_code IS NOT NULL AND failure_reason IS NOT NULL)
    );

CREATE INDEX idx_communications_alert ON communications (alert_id, created_at DESC);

CREATE UNIQUE INDEX uq_communications_active_draft_alert
  ON communications (alert_id)
  WHERE status = 'DRAFT' AND alert_id IS NOT NULL;
