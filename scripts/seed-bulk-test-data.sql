-- Dev-only bulk sample data for exercising list/pagination/aggregation
-- endpoints (e.g. GET /users/summary) against something bigger than the two
-- hand-crafted clinics in seed-dev-data.sql. Not a Flyway migration — run
-- manually against your local DB whenever you want a larger dataset:
--
--   docker exec -i medy-postgres-1 psql -U medy -d medy < scripts/seed-bulk-test-data.sql
--
-- Creates 20 organizations ("Test Clinic 01".."Test Clinic 20"), each with
-- 10 staff users: exactly one CLINIC_ADMIN plus 9 staff with a randomly
-- chosen role (DOCTOR/RECEPTIONIST/ASSISTANT/ACCOUNTANT). All passwords are
-- "password123" (same bcrypt hash used in seed-dev-data.sql).
--
-- Login as any generated clinic admin, e.g.:
--   POST /auth/login {"orgSlug": "test-clinic-01", "email": "test-clinic-01.staff1@test-data.local", "password": "password123"}
--
-- Not idempotent — running this twice creates 20 more orgs with a slug
-- collision (uq on organizations.slug), so it'll fail loudly rather than
-- silently duplicate. Safe to re-run only after clearing prior output, e.g.:
--   DELETE FROM users WHERE email LIKE '%@test-data.local';
--   DELETE FROM organizations WHERE slug LIKE 'test-clinic-%';

WITH new_orgs AS (
    INSERT INTO organizations (id, name, slug, created_at)
    SELECT gen_random_uuid(),
           'Test Clinic ' || lpad(i::text, 2, '0'),
           'test-clinic-' || lpad(i::text, 2, '0'),
           now()
    FROM generate_series(1, 20) AS i
    RETURNING id, slug
)
INSERT INTO users (id, tenant_id, email, password_hash, full_name, role, active)
SELECT gen_random_uuid(),
       o.id,
       o.slug || '.staff' || staff_no || '@test-data.local',
       '$2a$10$C/V85marDUGPkctbuk8Ksu7SWO6D8AFUAqmldCLb/ckhQ.Dz278ka',
       'Staff Member ' || staff_no,
       CASE
           WHEN staff_no = 1 THEN 'CLINIC_ADMIN'
           ELSE (ARRAY['DOCTOR', 'RECEPTIONIST', 'ASSISTANT', 'ACCOUNTANT'])[1 + floor(random() * 4)::int]
       END,
       true
FROM new_orgs o
CROSS JOIN generate_series(1, 10) AS staff_no;
