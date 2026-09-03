CREATE TABLE patients (
    id             UUID PRIMARY KEY,
    tenant_id      UUID         NOT NULL REFERENCES organizations (id),
    first_name     VARCHAR(255) NOT NULL,
    last_name      VARCHAR(255) NOT NULL,
    date_of_birth  DATE         NOT NULL
);

CREATE INDEX idx_patients_tenant_id ON patients (tenant_id);
