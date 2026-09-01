# Medy - Medical ERP & CRM Architecture & Implementation Plan

---

## 1. Executive Summary & Core Requirements

**Medy** is a multi-tenant, modular Medical ERP & CRM system tailored for medical clinics and multi-location healthcare networks (such as dental, polyclinic, and aesthetic clinics).

### Key Business & Technical Goals
1. **Modular Monetization & Feature Gating**: Enable/disable specific modules per tenant/clinic based on subscription tiers or customized contracts.
2. **Multi-Location & Chain Support**: Centralized administration with granular location-, doctor-, and cabinet-level segregation.
3. **Romanian & European Compliance & Integrations**:
   - **ANAF e-Factura SPV** (OAuth2, UBL 2.1 XML generation, validation, and status tracking).
   - **GDPR & Medical Data Compliance** (Consent management, encrypted audit logs, digital signatures for consents).
   - **Local & Global SMS Providers** (SMSLink, SendSMS, Twilio).
   - **Integrated Payments & Installments** (Stripe, POS, Cash, TBI Bank / BNPL, In-House Installments).
4. **Clean Backend Architecture**: Java 21+ / Spring Boot 3/4 with **Spring Modulith** to maintain strict module boundaries, high performance, and future microservice-readiness without premature distributed system complexity.

---

## 2. Backend Architecture: Spring Modulith Modular Monolith

### Why Spring Modulith?
- **Strict Boundary Enforcement**: Module packages are isolated; intra-module communication goes strictly through explicit public APIs or decoupled asynchronous domain events (`ApplicationEventPublisher`).
- **Dynamic Feature Entitlements**: Modules can be enabled or disabled dynamically at runtime per tenant through interceptors and licensing services.
- **Single Deployment Unit, Future-Proof**: Easy to deploy and maintain by small-to-medium teams, with zero network latency between modules, while allowing any module (e.g., e-Factura, SMS notifications) to be extracted into a standalone microservice if load requires it.

```
com.example.medy/
│
├── core/                         # Shared kernel, base entities, multi-tenancy context, security
│   ├── tenancy/                  # TenantContext, TenantFilter, LocationContext
│   ├── security/                 # JWT, RBAC/ABAC, SecurityUtils
│   ├── licensing/                # FeatureFlag / ModuleEntitlement Engine
│   └── audit/                    # Medical compliance audit trails
│
├── patient/                      # [Module] Management Pacienti & Medical Records
├── appointment/                  # [Module] Programari, Calendar & Resource Scheduling
├── treatment/                    # [Module] Planuri Tratamente, Odontogram & Procedures
├── billing/                      # [Module] Plati, Incasari, Doctor Commissions, ANAF e-Factura
├── installment/                  # [Module] Sisteme de plata in rate (TBI Bank, BNPL, In-House)
├── signature/                    # [Module] Semnatura digitala (Consent forms, biometric/canvas)
├── notification/                 # [Module] Notificare SMS & Email engine
├── recall/                       # [Module] Recall Pacienti (Automation & Periodic Follow-ups)
├── location/                     # [Module] Multi-location, Cabinets & Clinic Hierarchy
├── marketing/                    # [Module] Lead capture, Campaigns, Referral tracking
└── reporting/                    # [Module] Raportare, Analytics & KPI Dashboards
```

---

## 3. Module Breakdown & Domain Design

### 3.1. Tenant, Location & IAM (`core`, `location`)
- **Multi-Tenancy Strategy**: Discriminator-based (`tenant_id` on all entities) with Hibernate `@TenantId` or Hibernate Filters, accompanied by `TenantContextHolder` (extracted from JWT / Subdomain / Custom Header).
- **Location Hierarchy**:
  - `Organization` (Tenant / Clinic Group)
    └── `ClinicLocation` (City / Branch)
        └── `Cabinet` / `Room` / `Chair` (Resource allocated to appointments)
- **Role-Based Access Control (RBAC)**:
  - `SUPER_ADMIN` (Platform operator)
  - `CLINIC_ADMIN` (Full access to clinic configuration, finances, reporting)
  - `DOCTOR` / `PRACTITIONER` (Calendar, own patients, treatment plans, medical charts, commissions)
  - `ASSISTANT` / `NURSE` (Patient prep, vital signs, appointment updates)
  - `RECEPTIONIST` (Bookings, patient check-in, point-of-sale invoicing)
  - `ACCOUNTANT` (Financials, e-Factura, commissions, fiscal reports)

### 3.2. Dynamic Module Licensing & Entitlements
To prevent clients from accessing modules they haven't purchased:
1. **Tenant Entitlements Table**:
   - `tenant_id`, `module_code` (e.g. `ANAF_EFACTURA`, `RECALL`, `TREATMENT_PLANS`, `MARKETING`, `INSTALLMENTS`), `is_enabled`, `valid_until`.
2. **Spring AOP Annotation**:
   ```java
   @Target({ElementType.METHOD, ElementType.TYPE})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface RequiresModule {
       ModuleCode value();
   }
   ```
3. **Aspect / Filter Enforcement**:
   - Intercepts incoming API calls before reaching module controllers. If the current tenant's subscription lacks the `ModuleCode`, it immediately rejects the request with `403 Module Not Subscribed`.
   - UI receives a list of active modules upon login (`/api/v1/auth/me` or `/api/v1/tenant/features`) to hide inaccessible menu items and features.

---

### 3.3. Appointments & Calendar (`appointment`)
- **Features**:
  - Multi-view calendar (Day, Week, Month, Resource/Cabinet View, Doctor View).
  - Drag-and-drop rescheduling with conflict detection (Doctor overlap, Cabinet/Equipment overlap).
  - Status lifecycle: `REQUESTED` → `CONFIRMED` → `CHECKED_IN` → `IN_TREATMENT` → `COMPLETED` → `CANCELLED` / `NO_SHOW`.
  - Real-time updates via WebSockets / SSE for multi-receptionist concurrency.
  - Event publishing: `AppointmentCreatedEvent`, `AppointmentCancelledEvent` consumed by the `notification` module for SMS reminders.

---

### 3.4. Patient Management (`patient`)
- **Features**:
  - Patient master record (CNP/ID validation, demographic details, contact info, insurance status).
  - Medical history / Anamnesis (Allergies, chronic conditions, medication, contraindications).
  - GDPR consent logs and family connections (e.g., parent managing child profile).
  - Patient timeline (Past appointments, treatments, billing history, SMS logs, signed documents).

---

### 3.5. Treatment Plans (`treatment`) — DECIDED: Configurable Multi-Specialty

To avoid hardcoding a dental-only model (and a rewrite if Medy expands beyond dentistry), charting and procedure definitions are **config-driven** rather than fixed schema:
- **Procedure Catalog**: per-tenant/per-specialty catalog of procedures (code, name, default price, duration, category), stored relationally; no hardcoded "tooth number" or dental-specific columns.
- **Charting Engine**: a generic **chart template** model (JSONB) — a tenant can configure a "chart" as a dental odontogram (32-tooth grid), a body-map (dermatology/aesthetics), or a plain procedure list (general medicine). The `treatment` module stores chart *instances* as structured JSON validated against the tenant's active template, not as fixed columns.
- Staged treatments (Stage 1: Urgency → Stage 2: Core Treatment → Stage 3: Aesthetics).
- Acceptance status (`PROPOSED`, `ACCEPTED_BY_PATIENT`, `IN_PROGRESS`, `FINISHED`).
- Automatic transfer of completed treatment stages into the `billing` module for invoice generation.
- **Tradeoff accepted**: more upfront modeling work (template engine + JSON validation) instead of a quick dental-only schema, in exchange for not needing a data-model rewrite to sell into non-dental clinics later.

---

### 3.6. Billing, Doctor Commissions & ANAF e-Factura (`billing`)
- **Payments & Receipts**:
  - Invoicing (Proforma & Fiscal Invoices, Receipts/Chitanțe).
  - Payment methods: Cash, POS Card, Stripe online payment links, Bank Transfer.
- **Doctor Commissions**:
  - Configurable commission models (Flat percentage per doctor, tiered commission based on procedure category, deduction of consumable costs before commission).
  - Automated calculation on completed & paid treatments.
  - Monthly payout and commission settlement reports.
- **ANAF e-Factura Integration — DECIDED: Pluggable, both providers**:
  - A provider-agnostic `FiscalInvoicingProvider` interface (`submit(invoice)`, `pollStatus(uploadId)`, `fetchValidationArchive(uploadId)`) with two implementations selectable per tenant:
    1. **Direct ANAF SPV**: OAuth2 token management with the SPV digital certificate, UBL 2.1 XML generation compliant with Romanian legislation, upload to the SPV queue, polling for validation index (`ID încărcare`, `Stare: OK / Erori`), storage of signed validation ZIP files.
    2. **Provider API fallback** (e.g., SmartBill / FGO): delegates XML generation and SPV submission to a third-party billing API, trading some control for faster integration and less compliance-edge-case ownership.
  - Tenant configuration picks which implementation is active; both conform to the same interface so the rest of `billing` (invoice generation, commission payout, reporting) is provider-agnostic.

---

### 3.7. Integrated Installment Payment Systems (`installment`)
- **Features**:
  - **Financing Partners (BNPL / Credit)**: Integration with Romanian healthcare credit providers (e.g., TBI Bank, Leanpay, or Stripe BNPL).
  - **In-House Clinic Installment Plans**:
    - Creation of installment schedules for expensive treatment plans (e.g., 3-12 monthly installments).
    - Down payment tracking, remaining balance calculation, auto-reminders for due dates.
    - Integration with SMS/Email notifications when an installment is due.

---

### 3.8. Digital Signature & Medical Consents (`signature`)
- **Features**:
  - PDF template engine for medical consent forms, GDPR agreements, and treatment plan acceptances.
  - Digital signature capture:
    - **In-clinic signature**: Tablet / Touchscreen signature canvas capture with timestamp, IP, and biometric pressure metadata.
    - **Remote signature**: Secure tokenized link sent via SMS/Email for patient pre-signing before arrival.
  - Cryptographic timestamping and immutable PDF archival.

---

### 3.9. SMS Notifications & Patient Recall (`notification`, `recall`)
- **SMS Notifications (`notification`)**:
  - Multi-provider gateway adapter (SMSLink.ro, SendSMS.ro, Twilio).
  - Automated triggers:
    - Appointment booking confirmation.
    - Reminder 24h & 2h before the appointment with quick confirmation link or two-way reply.
    - Payment receipt / invoice link.
    - Installment due reminders.
- **Patient Recall Engine (`recall`)**:
  - Automated rule engine for recurring medical visits:
    - Dental periodic cleaning & checkup (e.g., every 6 months).
    - Post-surgery / Post-procedure follow-ups (e.g., 7 days, 30 days).
    - Inactive patient re-engagement (no visit in >12 months).
  - Dedicated receptionist queue for calling or auto-triggering SMS recall campaigns.

---

### 3.10. Multi-Location Management (`location`)
- **Features**:
  - Centralized management of clinic branches.
  - Doctor schedules across multiple locations (e.g., Dr. X works Mon/Wed at Location A, Tue/Thu at Location B).
  - Location-scoped reporting, inventory, and fiscal series (Invoices series A for Location 1, series B for Location 2).

---

### 3.11. Marketing & Lead CRM (`marketing`)
- **Features**:
  - Web lead capture endpoints (for clinic landing pages and contact forms).
  - Campaign attribution (UTM parameters, Google Ads / Facebook Ads source tracking).
  - Promotional discount codes & patient referral tracking.
  - Conversion funnel from Lead → Appointment Scheduled → Treatment Accepted.

---

### 3.12. Reporting & Statistics (`reporting`)
- **Features**:
  - **Financial KPIs**: Gross revenue, collected revenue, outstanding balances, doctor commission summaries, average ticket size.
  - **Operational KPIs**: Cabinet occupancy rate, doctor utilization rate, appointment cancellation & no-show rate.
  - **Medical & Patient KPIs**: New vs. returning patients, treatment plan acceptance rate, recall success rate.

---

## 4. Frontend Technology — DECIDED: React / Next.js

**Decision driver**: a future doctor-facing and/or patient-facing **mobile app** is on the roadmap. React → React Native shares hooks, API client logic, validation, and often component patterns between web and mobile; Angular's mobile paths (Ionic/NativeScript) don't offer comparable reuse or a truly native feel. Since switching frontend frameworks later is far more expensive than the initial learning curve now, React/Next.js is the call for both surfaces.

1. **Admin / Clinic Staff Portal (ERP/CRM)**:
   - **Next.js (App Router) + React 19**, TanStack Query/Table for data-heavy grids, shadcn/ui + Tailwind for components, FullCalendar (React) for scheduling views.
   - Mostly CSR/SPA-like behavior behind auth for the dashboard; SSR reserved for public-facing pages.
2. **Patient Self-Service / Public Booking / Clinic Landing Pages**:
   - Same Next.js app (or a separate lightweight app) for SEO-optimized booking and marketing pages, hitting the same Java backend.
3. **Future Mobile (Doctor / Patient app)**:
   - **React Native (Expo)**, sharing API clients, hooks, and business logic with the web app where practical (e.g. via a shared TypeScript package in a monorepo).

---

## 5. Technical Stack & Architecture Blueprints

### Backend Stack
- **Language & Runtime**: Java 21 LTS (or Java 26 as configured)
- **Framework**: Spring Boot 3.x / 4.x
- **Architecture**: Spring Modulith (Modular Monolith)
- **Database**: PostgreSQL 16+ (JSONB for dynamic/configurable medical forms, procedure catalogs, and specialty-specific charting data — see §4-bis specialty decision)
- **ORM / Migrations**: Spring Data JPA / Hibernate + Flyway (database versioning)
- **Security**: Spring Security 6 (OAuth2 Resource Server / JWT)
- **Documentation**: Springdoc OpenAPI / Swagger UI
- **Queue / Scheduling**: Spring `@Scheduled` / Spring Modulith Event Publication Registry / RabbitMQ or Redis (as scale increases)

### Multi-Tenancy & Security Model
```
[Client Request] 
       │
       ▼
[JwtAuthenticationFilter] ──► Extracts User, Roles & TenantId
       │
       ▼
[TenantContextFilter] ──────► Sets ThreadLocal TenantContext & LocationContext
       │
       ▼
[ModuleSecurityAspect] ─────► Checks @RequiresModule(ModuleCode) against Tenant Entitlements
       │
       ▼
[Controller / Service] ─────► Business Logic (Spring Modulith Module)
       │
       ▼
[JPA Repository] ───────────► Applies TenantId Discriminator / Hibernate Filter automatically
```

---

## 6. Implementation Roadmap

### Phase 1: Foundation & Core Infrastructure
- Setup Multi-Tenancy (`TenantContext`, JPA tenant filtering).
- Setup User & Role Authentication (JWT, RBAC).
- Implement Module Entitlements Engine (`@RequiresModule`).
- Setup Multi-Location, Cabinets, and Staff domain model.

### Phase 2: Patient CRM & Appointment Calendar
- Patient Master Record & Medical History (Anamnesis).
- Dynamic GDPR & Medical Consent form definitions.
- Appointment Management & Multi-Cabinet / Multi-Doctor Calendar.
- SMS Notification Adapter & automated appointment reminders.

### Phase 3: Medical Treatment Plans & Digital Signature
- Medical procedure catalog & price lists.
- Interactive Treatment Plans (staged treatment steps, costs).
- In-clinic & Remote Digital Signature capture for consents and plans.

### Phase 4: Billing, Commissions & Financial Integrations
- Invoicing & Point-of-Sale receipt generation.
- Doctor commission engine with customizable calculation rules.
- ANAF e-Factura SPV integration (UBL XML generation & OAuth2 sync).
- Installment payment module (In-house schedules & TBI Bank/BNPL workflows).

### Phase 5: Recall, Marketing & Analytics
- Patient Recall Engine (automated periodic checks & follow-up queues).
- Lead capture API, campaign tracking & referral management.
- Operational & Financial Reporting dashboards.

---

## 7. Confirmed Architecture Decisions

| # | Decision | Choice | Rationale |
| :-- | :--- | :--- | :--- |
| 1 | Database | **PostgreSQL 16+** | JSONB needed for the configurable chart-template engine (§3.5) and dynamic consent/questionnaire forms. |
| 2 | ANAF e-Factura strategy | **Pluggable, both** (direct SPV + provider fallback) | No lock-in; per-tenant choice between full control (direct SPV) and faster/lower-maintenance integration (SmartBill/FGO). |
| 3 | Frontend | **React / Next.js** (web), **React Native/Expo** (future mobile) | A doctor/patient-facing mobile app is on the roadmap; React → React Native gives real code/logic reuse that Angular's mobile story (Ionic/NativeScript) doesn't match. Steeper initial learning curve accepted as a one-time cost. |
| 4 | Clinic specialty focus | **Configurable multi-specialty** | Procedure catalog and charting are config/template-driven from day one (§3.5) to avoid a data-model rewrite when selling beyond dental clinics. |
| 5 | SMS provider | **Deferred — pluggable multi-provider adapter** | Architecture already isolates this behind an adapter interface (§3.9); picking SMSLink vs. SendSMS vs. Twilio is a low-risk, late-bindable decision — revisit when the `notification` module is built. |

These are locked for the purposes of starting implementation. Anything not yet decided (e.g. specific commission calculation rules, installment provider selection, exact RBAC permission matrix) will surface as its own focused decision when that module's phase begins, rather than being guessed at up front.
