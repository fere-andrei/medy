CREATE TABLE users (
    id             UUID PRIMARY KEY,
    tenant_id      UUID REFERENCES organizations (id),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL
);

-- Staff: email unique within their own clinic.
CREATE UNIQUE INDEX uq_users_tenant_email ON users (tenant_id, email) WHERE tenant_id IS NOT NULL;

-- Super admins (no tenant): email unique platform-wide.
CREATE UNIQUE INDEX uq_users_global_email ON users (email) WHERE tenant_id IS NULL;
