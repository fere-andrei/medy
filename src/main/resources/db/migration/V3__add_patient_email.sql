ALTER TABLE patients ADD COLUMN email VARCHAR(255);

CREATE UNIQUE INDEX uq_patients_tenant_email ON patients (tenant_id, email) WHERE email IS NOT NULL;
