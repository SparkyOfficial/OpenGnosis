-- IAM Service Initial Schema
-- Users table
CREATE TABLE iam.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_email ON iam.users(email);
CREATE INDEX idx_users_status ON iam.users(status);

-- Roles table
CREATE TABLE iam.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role_name CHECK (name IN ('STUDENT', 'TEACHER', 'PARENT', 'ADMINISTRATOR', 'SYSTEM_ADMIN'))
);

-- User roles junction table
CREATE TABLE iam.user_roles (
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON iam.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON iam.user_roles(role_id);

-- Permissions table
CREATE TABLE iam.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Role permissions junction table
CREATE TABLE iam.role_permissions (
    role_id UUID NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES iam.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Refresh tokens table
CREATE TABLE iam.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user_id ON iam.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON iam.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON iam.refresh_tokens(expires_at);

-- Insert default roles
INSERT INTO iam.roles (name, description) VALUES
    ('STUDENT', 'Student role with access to view grades and assignments'),
    ('TEACHER', 'Teacher role with access to manage grades and attendance'),
    ('PARENT', 'Parent role with access to view child information'),
    ('ADMINISTRATOR', 'Administrator role with access to manage school structure'),
    ('SYSTEM_ADMIN', 'System administrator with full access');

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION iam.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON iam.users
    FOR EACH ROW EXECUTE FUNCTION iam.update_updated_at_column();
