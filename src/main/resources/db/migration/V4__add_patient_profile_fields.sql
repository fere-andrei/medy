ALTER TABLE patients
    ADD COLUMN sex                     VARCHAR(10),
    ADD COLUMN national_id              VARCHAR(20),
    ADD COLUMN phone_number             VARCHAR(30),
    ADD COLUMN address_line             VARCHAR(255),
    ADD COLUMN city                     VARCHAR(255),
    ADD COLUMN county                   VARCHAR(255),
    ADD COLUMN postal_code              VARCHAR(20),
    ADD COLUMN country                  VARCHAR(100),
    ADD COLUMN insurance_provider       VARCHAR(255),
    ADD COLUMN insurance_number         VARCHAR(100),
    ADD COLUMN emergency_contact_name   VARCHAR(255),
    ADD COLUMN emergency_contact_phone  VARCHAR(30);

CREATE UNIQUE INDEX uq_patients_tenant_national_id ON patients (tenant_id, national_id) WHERE national_id IS NOT NULL;
