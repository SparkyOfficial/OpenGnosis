-- Notification Preferences Table
CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- Notification Type Preferences Table
CREATE TABLE notification_type_preferences (
    user_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, notification_type),
    FOREIGN KEY (user_id) REFERENCES notification_preferences(user_id) ON DELETE CASCADE
);

-- Notification Delivery Table
CREATE TABLE notification_delivery (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0
);

-- Device Tokens Table
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_type VARCHAR(50) NOT NULL,
    registered_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Indexes for performance
CREATE INDEX idx_notification_delivery_user_id ON notification_delivery(user_id);
CREATE INDEX idx_notification_delivery_status ON notification_delivery(status);
CREATE INDEX idx_notification_delivery_sent_at ON notification_delivery(sent_at DESC);
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_token ON device_tokens(token);
CREATE INDEX idx_device_tokens_active ON device_tokens(active);
