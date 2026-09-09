CREATE TABLE appointments (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES organizations (id),
    patient_id  UUID          NOT NULL,
    doctor_id   UUID          NOT NULL,
    start_time  TIMESTAMP     NOT NULL,
    end_time    TIMESTAMP     NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    notes       VARCHAR(1000)
);

CREATE INDEX idx_appointments_tenant_id ON appointments (tenant_id);
CREATE INDEX idx_appointments_doctor_id_start_time ON appointments (doctor_id, start_time);
