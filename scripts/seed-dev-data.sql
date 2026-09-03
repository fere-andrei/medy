-- Dev-only sample data. Not a Flyway migration — run manually against your
-- local DB (psql, or IntelliJ's Database console) whenever you want fresh
-- sample organizations/patients to test against.
--
--   docker exec -i medy-postgres-1 psql -U medy -d medy < scripts/seed-dev-data.sql

INSERT INTO organizations (id, name, slug, created_at) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Clinica Dentara Sorin', 'clinica-sorin', now()),
    ('a0000000-0000-0000-0000-000000000002', 'Policlinica ProVita', 'policlinica-provita', now());

INSERT INTO patients (
    id, tenant_id, first_name, last_name, date_of_birth, gender, national_id, email,
    phone_number, address_line, city, county, postal_code, country,
    insurance_provider, insurance_number, emergency_contact_name, emergency_contact_phone
) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'Ana', 'Popescu', '1990-05-12', 'FEMALE',
     '2900512123456', 'ana.popescu@example.com', '0722111222', 'Str. Florilor 12', 'Cluj-Napoca', 'Cluj',
     '400001', 'Romania', 'CNAS', 'INS-1001', 'Mihai Popescu', '0722111333'),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'Mihai', 'Ionescu', '1985-11-03', 'MALE',
     '1851103123456', 'mihai.ionescu@example.com', '0733222333', 'Str. Lalelelor 5', 'Cluj-Napoca', 'Cluj',
     '400002', 'Romania', 'CNAS', 'INS-1002', 'Elena Ionescu', '0733222444'),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'Elena', 'Radu', '1978-02-20', 'FEMALE',
     '2780220123456', 'elena.radu@example.com', '0744333444', 'Bd. Unirii 20', 'Bucuresti', 'Bucuresti',
     '030001', 'Romania', 'Privat Asig', 'INS-2001', 'Ion Radu', '0744333555'),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'Vasile', 'Georgescu', '1995-07-30', 'MALE',
     '1950730123456', 'vasile.georgescu@example.com', '0755444555', 'Str. Victoriei 8', 'Bucuresti', 'Bucuresti',
     '030002', 'Romania', 'Privat Asig', 'INS-2002', 'Maria Georgescu', '0755444666');
