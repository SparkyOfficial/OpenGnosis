-- Command Audit Log Table
CREATE TABLE command_audit_log (
    id UUID PRIMARY KEY,
    command_type VARCHAR(100) NOT NULL,
    issued_by UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_status CHECK (status IN ('ACCEPTED', 'REJECTED', 'FAILED'))
);

-- Indexes for performance
CREATE INDEX idx_command_audit_log_issued_by ON command_audit_log(issued_by);
CREATE INDEX idx_command_audit_log_timestamp ON command_audit_log(timestamp);
CREATE INDEX idx_command_audit_log_status ON command_audit_log(status);
CREATE INDEX idx_command_audit_log_command_type ON command_audit_log(command_type);

-- Unique constraint to ensure idempotency
CREATE UNIQUE INDEX idx_command_audit_log_id ON command_audit_log(id);
