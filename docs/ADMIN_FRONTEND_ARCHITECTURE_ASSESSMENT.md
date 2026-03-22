# Architecture & functionality assessment — Admin Frontend planning

**Scope:** Backend and shared infrastructure present in the workspace roots; frontend detail **only** for `investor-portal` and `rightsissue`. **Out of scope for frontend detail:** Reporting UI, ipo-ui, fix-middleware-ui, gateway-ui-next, ReconciliationUI (noted briefly where relevant to backends).

**Date:** March 21, 2026

---

## A. Executive summary

### Workspace shape

The trading platform is a **multi-service Spring Boot ecosystem** centered on:

- **`Viken Gateway`** (`spring.application.name=Viken Gateway`, port `10102`) — HTTP entry point using **Apache Camel** with **database-driven dynamic routes** (`DynamicRouteBuilder`), **Redis** rate limiting, **dual PostgreSQL** (operational + audit), JWT validation against **IAM**.
- **`iam-service`** — OAuth2 authorization server, cookie/session and JWT, **PostgreSQL** + audit DB, optional **Kafka** for OTP-related flows, **Redis** rate limits.
- **Domain services** each with their own PostgreSQL (and in some cases additional SQL Server DBs for legacy CSD/ATS/MMH): **onboarding**, **nida**, **IPO**, **rights issue**, **reconciliation**, **reporting**, **trading-core**, **investor-holding-service (ihs)**, **notification**, **fix-middleware**, **OMS** (`order-management-service` under `rev-eng-mtp/projects/`, not under `Backend/Trading/oms`).
- **Async integration** is predominantly **Kafka** (order events, FIX bridges, allocations, notifications).
- **`viken-chaos-monkey`** — operational tooling; **chaos disabled by default** in config.

### Business domains covered

Identity & access; investor onboarding & KYC/NIDA; market/trading reference data and validation (**trading-core**); order lifecycle & payments (**OMS**); FIX connectivity to exchange (**fix-middleware**); holdings (**ihs**); IPO & rights-issue corporate actions; reporting & contract notes; reconciliation across MTP/CSD/ATS/MMH; notifications (email/SMS/Kafka).

### Active vs inactive (evidence-based)

| Category | Examples |
|----------|----------|
| **Active / substantial** | `gateway`, `iam`, `onboarding`, `nida`, `ipo`, `rightsissue`, `Reconciliation`, `Reporting`, `trading-core`, `investor-holding-service`, `notification`, `fix-middleware`, `order-management-service`, `ReconciliationUI` (Next app with tests/OpenAPI), `investor-portal`, `rightsissue` (Next UIs) |
| **Partial / stub** | `analytics-service` (only `AnalyticsApplication` + `HealthController` in tree) |
| **Placeholder / empty / artifact** | `Backend/Trading/oms` — **only `.idea` files, no source** (OMS lives in `rev-eng-mtp/projects/order-management-service`). `mtp-fat-jar` / decompiled trees — **reverse-engineering artifacts**, not operational services. |
| **Template / not product** | `Frontend/admin` — **Metronic starter** (hundreds of layout demo pages). `Frontend/admin-source` — **Metronic + Prisma + NextAuth** demo (generic user-management API routes), **not** wired to DSE IAM/gateway domain. |

### Key findings for a new Admin Frontend

1. **Admin capabilities are already split across backends:** IAM (users/roles/permissions), rights-issue module (issuer ops + help CMS + permissions like `rightsissue:admin:...`), reconciliation/reporting (ops, multi-DB reads), gateway (routing/audit/rate limits), OMS/trading-core/fix (trading ops). There is **no single “admin BFF”** in the reviewed code; clients are expected to call **gateway-prefixed** service paths.
2. **`rightsissue` frontend is the closest existing “staff/admin” UI** — extensive `/admin/*` and CDS-only flows, IAM permission gates — but it is **rights-issue–scoped**, not platform-wide.
3. **`investor-portal`** is **retail/broker/custodian/foreign**-oriented; it uses **Next.js proxy routes** to the gateway (`BACKEND_API_BASE_URL`, default `http://localhost:10102`) and **cookie-based IAM** — a strong pattern to reuse for any browser admin app (avoid CORS, forward `access_token`).
4. **`admin` / `admin-source` are not suitable as a base for production admin product logic** without major replacement of auth and data layers; they are **commercial template code**.

---

## B. Backend microservices inventory

Below: **service name** = `spring.application.name` where read from `application.properties` (or obvious from code).

### 1. Viken Gateway

**Path:** `Backend/Trading/gateway`

**Business role:** Single **edge API** for browsers and services: **routing**, **authZ** (JWT), **rate limiting**, **audit** (separate DB), optional **service discovery** integration.

**Main modules / features (from code/config):** Camel **dynamic routes** from DB (`DynamicRouteBuilder` — builds servlet consumers per registered `basePath`); dead-letter handling; outbound audit processor; investor dashboard controller exists under `modules/investor` (gateway-hosted surface area).

**Key integrations:** PostgreSQL (primary + `gateway_audit`), **Redis**, **IAM** as token issuer (`auth.server.issuer-uri`, e.g. `http://localhost:10102/iam/api`).

**Dependencies on other services:** Does not own business domains; **proxies** to registered downstream URLs. **Consumes** IAM for JWT validation.

**APIs exposed:** HTTP proxy to `/{service}/api/v1/...` style paths (comment in gateway config: *service-first routing*).

**Data ownership:** Gateway DB = **routes, operational config, audit metadata** — not investor/order truth.

**Maturity:** **High** — Flyway, dual DB, explicit security and rate-limit config.

**Relevance to Admin Frontend:** **Central** — all admin UI calls should assume **gateway as the browser-facing API** (aligns with `investor-portal` and `rightsissue` proxy patterns).

---

### 2. iam-service

**Path:** `Backend/Trading/iam`

**Business role:** **Identity & access**: login, refresh, OAuth2/OIDC-style endpoints, **roles/permissions** in JWT, password flows.

**Main modules:** `WebSessionController`, `SessionCookieController`, `TokenAuthController`, `UserRoleController`, etc.

**Key integrations:** PostgreSQL + **iam_audit**; **Redis** (rate limit); **Kafka** (optional `APP_KAFKA_ENABLED`) for notification-side effects; JWT audience lists include `order-management-service`, `reporting`, `fix-middleware`, `rightsissue` (see `iam.token.audience`).

**Dependencies:** May **produce** events to Kafka for OTP; **consumed by** all resource servers via JWKS.

**APIs exposed:** Under `server.servlet.context-path=/api` → e.g. `/api/v1/auth/*` (via gateway: `/iam/api/v1/auth/*`).

**Data ownership:** Users, credentials, roles, permissions, sessions (as designed).

**Maturity:** **High** — Flyway, audit migrations, explicit cookie auth toggles.

**Relevance to Admin Frontend:** **Primary** for **admin user provisioning, roles, permission codes** for fine-grained UI (e.g. rights-issue admin permissions already exist in IAM migrations / codes consumed by `rightsissue` UI).

---

### 3. trading-onboarding

**Path:** `Backend/Trading/onboarding`

**Business role:** **Investor onboarding** pipeline: KYC flows, customer records, coordination with **trading-core** / CDS concepts.

**Main modules:** Resident KYC caches/services, Kafka producers for `onboarding.completed` and `investor.trading.account`.

**Key integrations:** PostgreSQL (`onboarding` DB), **Redis**, **Kafka**, HTTP to **IAM** (`onboarding.iam.*`), configurable **trading-data** and **trading-csd** base URLs.

**Dependencies:** **Calls IAM** (admin client credentials in config for service use); **trading-core consumes** `onboarding.completed` (see trading-core `application.properties`).

**APIs:** REST + Swagger (`springdoc.*` enabled).

**Data ownership:** Onboarding cases, KYC state — **not** the ledger of orders.

**Maturity:** **Mixed** — explicit **TODO in config: no Flyway yet**, `spring.jpa.hibernate.ddl-auto=update` (risk for production parity).

**Relevance to Admin Frontend:** **Ops dashboards** for onboarding queues, KYC status, manual intervention — **APIs exist**, schema governance needs hardening before heavy admin investment.

---

### 4. trading-nida

**Path:** `Backend/Trading/nida`

**Business role:** **National ID integration** (encryption, external NIDA URL config).

**Key integrations:** PostgreSQL, Redis, **external HTTPS** `nida.nin-url` (placeholder-style host in default config).

**Dependencies:** Consumed by onboarding/KYC flows (frontends proxy `/nida/` in `investor-portal` middleware).

**Maturity:** **Medium** — `ddl-auto=update`, no Flyway called out in snippet.

**Relevance to Admin Frontend:** Support tooling for **verification failures**, config of NIDA endpoints — **limited admin surface** unless you add operational controllers.

---

### 5. dse-ipo-module

**Path:** `Backend/Trading/ipo`

**Business role:** **IPO** subscriptions, allocations, payments integration, **Kafka** integration.

**Key integrations:** PostgreSQL, **Redis** cache, **mail**, **Kafka**, core API client classes.

**Dependencies:** **IAM** JWT resource server patterns; may call **trading-core** (`CoreApiClient`).

**APIs:** `/api` context-path, port default `10108` in properties comment.

**Maturity:** **High** — Flyway, `ddl-auto=validate`.

**Relevance to Admin Frontend:** **IPO operations** (issues, subscriptions, payment reconciliation) — natural admin module.

---

### 6. rights-issue-module

**Path:** `Backend/Trading/rightsissue`

**Business role:** **Rights issues** lifecycle: eligibility, subscriptions, allocations, payments, **help/CMS** for documentation, audit.

**Key integrations:** PostgreSQL, Redis, **CDS** integration properties, Kafka topics for corporate actions, **IAM** JWT with audience `rightsissue`.

**Dependencies:** IAM permissions; events to **ihs** (`rights-issue.allocation.confirmed` consumed per `investor-holding-service` config).

**Maturity:** **High** — Flyway, structured management endpoints, metrics.

**Relevance to Admin Frontend:** **Very high** — backend already models **admin** and **CDS-only** concerns; frontend `rightsissue` mirrors this.

---

### 7. reconciliation-service

**Path:** `Backend/Trading/Reconciliation`

**Business role:** **Position/trade reconciliation** across **MTP (PostgreSQL)** and **CSD / ATS / MMH (SQL Server)**; operational toggles via **SystemConfig** API (described in header comments of `application.properties`).

**Key integrations:** Multiple datasources (primary PG + MTP + CSD + ATS + MMH), **Redis**, **Caffeine** cache, Flyway on primary only.

**Dependencies:** Reads legacy exchange databases; may trigger notifications/webhooks (config keys in comments).

**Maturity:** **High** for integration breadth; **env-heavy** (many required DB env vars).

**Relevance to Admin Frontend:** **Back-office reconciliation dashboards**, exception queues, config toggles — **core admin domain**.

---

### 8. reporting-service

**Path:** `Backend/Trading/Reporting`

**Business role:** **Investor reporting** — contract notes, statements, **jOOQ** for heavy reads, multi-DB (same pattern as reconciliation: primary + CSD/ATS/MMH/MTP).

**Key integrations:** PostgreSQL primary, SQL Server secondary DBs, Redis, Flyway.

**Maturity:** **High** — explicit performance tuning, jOOQ feature flag.

**Relevance to Admin Frontend:** **Reporting ops** (regeneration, audit of downloads) — may overlap with investor-facing Reporting UI (excluded from this review).

---

### 9. trading-core-service

**Path:** `Backend/Trading/trading-core/trading-core-service`

**Business role:** **Reference data & trading rules**: securities, tick sizes, **order status matrix**, **user context** / investor trading account, **ATS** web service sync, **Kafka** consumers/producers (`investor.trading.account`, `onboarding.completed`, `fix.part-details.report`, etc.).

**Key integrations:** PostgreSQL `core`, Redis, Kafka, optional **OTLP** tracing.

**Dependencies:** Consumes onboarding events; **OMS** and **fix-middleware** integrate via shared Kafka topics and reference data.

**Maturity:** **High**.

**Relevance to Admin Frontend:** **Market ops** (securities, sessions, caps, schedules), **data stewardship** — many admin endpoints are likely here and in fix-middleware.

---

### 10. ihs (investor-holding-service)

**Path:** `Backend/Trading/investor-holding-service`

**Business role:** **Investor holdings** ledger updates from **OMS fills**, IPO allocation, rights allocation, order status events.

**Key integrations:** PostgreSQL, Kafka (topics documented in config), IAM JWT.

**Maturity:** **High** — explicit consumer design notes in config (ExecutionReport consumer disabled in favor of OMS status events).

**Relevance to Admin Frontend:** **Position/holding investigations**, corporate-action-driven adjustments — support tooling.

---

### 11. trading-notification

**Path:** `Backend/Trading/notification`

**Business role:** **Email/SMS** delivery; **Kafka**-driven consumers (`KafkaEmailListener`, `KafkaSmsListener`).

**Key integrations:** PostgreSQL, Kafka, SMTP, **Infobip**, **Slack** webhook; **Camel** `context-path=/api/v1/*`. **Flyway disabled** in main properties (`spring.flyway.enabled=false`) — operational implication.

**Dependencies:** **Producers** across IAM, OMS, others via Kafka topics.

**Maturity:** **Medium** — `spring.security.user.*` basic auth in config (dev-style); production should lock down.

**Relevance to Admin Frontend:** **Notification monitoring**, template management (if exposed), dead-letter handling — confirm APIs.

---

### 12. fix-middleware-service

**Path:** `Backend/Trading/fix-middleware`

**Business role:** **FIX session** to exchange/STT: order capture, execution reports, Kafka bridges to OMS, **GraphQL** controller, **STT** integration.

**Key integrations:** PostgreSQL (QuickFIX/J JDBC store), Redis, Kafka, **FIX** TCP to `FIX_SERVER_HOST`/`PORT`.

**Dependencies:** **OMS** via Kafka; **trading-core** for validation references.

**Maturity:** **High** — extensive FIX and operational docs in repo.

**Relevance to Admin Frontend:** **Market ops / broker ops** consoles (sessions, caches, security definitions) — **high** for exchange operations.

---

### 13. order-management-service (OMS)

**Path:** `rev-eng-mtp/projects/order-management-service` (**not** `Backend/Trading/oms`)

**Business role:** **Order management**, payments control numbers, Kafka **event-driven** processing, audit DB.

**Key integrations:** PostgreSQL `oms` + `oms_audit`, Redis, Kafka (many topics including `fix.order.*`, `oms.order.*`), IAM JWT.

**Dependencies:** **fix-middleware**, **trading-core**, **notification**, **ihs** via Kafka.

**Maturity:** **High** — dual Flyway configs, feature flags for processing mode.

**Relevance to Admin Frontend:** **Trading desk / order admin** — central.

---

### 14. msmq-manager

**Path:** `Backend/Trading/msmq`

**Business role:** **MSMQ** bridge/management (Windows messaging integration) — queue sync, monitoring.

**Key integrations:** **Redis** (remote cloud host in committed `application.properties` — **sensitive**), Swagger, custom actuator endpoint.

**Dependencies:** Likely **peripheral** to core trading API path unless workflows still use MSMQ.

**Maturity:** **Niche** — profile `spring.profiles.active=prod` in default file suggests environment-specific deployment.

**Relevance to Admin Frontend:** **Low** unless your org still operates MSMQ workflows; would be a **separate admin surface** if needed.

---

### 15. viken-chaos-monkey

**Path:** `Backend/Trading/viken-chaose-mokey`

**Business role:** **Chaos engineering** (disabled by default: `chaos.enabled=false`).

**Maturity:** Tooling; not business domain.

**Relevance to Admin Frontend:** Optional **SRE admin** integration only.

---

### Other directories (brief)

- **`Backend/Trading/analytics-service`:** **Stub** (health + main class only).
- **`Backend/Trading/oms`:** **Empty** of application code — **do not treat as OMS**.
- **`rev-eng-mtp/projects/decompiled_output/mtp-fat-jar` / `mtp-fat-jar`:** **Decompilation / legacy MTP** — not a deployable microservice as-is.
- **`infrastructure`, `infra/kafka`, `infra/rabitmq`, `devops/*`:** **Platform** — brokers, jobs, charts, not domain microservices.

---

## C. Frontend review (investor-portal & rightsissue only)

### 1. investor-portal (`Frontend/investor-portal`)

**Target users (evidence):** Login page presents **Retail, Broker, Custodian, Foreign** roles — **investors and intermediaries**, not a generic platform admin.

**Business flows supported (from `app/` routes):** Registration & KYC steps (NIDA, biometrics), **login/OTP**, **dashboard**, **stocks** (buy/sell, orders), **IPO** subscription/payment/allocation, **rights issues** list, **CDS** link/request, **account** management (minors, broker change), **transactions**, **market** pages.

**Backend services consumed (evidence):** Next **proxy routes** under `app/iam`, `app/core`, `app/oms`, `app/ipo`, `app/onboarding`, `app/nida`, `app/rightsissue`, `app/ihs` — forwarded to `BACKEND_API_BASE_URL` (default `http://localhost:10102`). Client API modules: `lib/api/auth.ts` → **`/iam/api/v1/auth`**, `trading.ts` → **`/core/api/v1/...`**, plus onboarding, IPO, CDS, NIDA, holdings.

**Authentication / authorization:** **Cookie-based IAM** (`access_token` cookie); `middleware.ts` protects UI routes while allowing `/api/` and service prefixes that include `/api/`. **401** handling in `http-client.ts` coordinates with `SessionExpiryGuard` pattern. JWT payload used for **roles/permissions** in auth helpers.

**Reusable patterns for Admin Frontend:**

- **Gateway proxy composition** (same-origin API routes, no CORS).
- **Centralized auth** + **permission parsing** from JWT.
- **Axios** clients with **withCredentials**.

**Limitations for admin:** **No IAM user/role management UI** in tree; **no reconciliation/reporting** modules; **role picker** is investor/broker-focused, not staff RBAC.

---

### 2. rightsissue (`Frontend/rightsissue`)

**Target users:** **Staff** running rights issues — routes under `app/(dashboard)/admin/*`, **CDS-only** flows, **help** CMS — plus investor-facing **help** reading.

**Business flows:** Full **rights issue lifecycle** (create/edit/draft/active/closed), **subscriptions**, **eligibility**, **allocations**, **payments**, **entitlements**, **audit**, **reports** (dashboard, reconciliation, aging, etc.), **admin help** CRUD.

**Backend services consumed:** `lib/api.ts` uses `NEXT_PUBLIC_API_BASE_URL || '/api'` and IAM paths; **`app/iam/[...path]/route.ts`** proxies to gateway with **Basic auth for token endpoint** (gateway client). **Rights issue API** via same gateway pattern.

**Authentication / authorization:** **localStorage** session flags + user (`SESSION_KEY`, `USER_KEY`) and **permission helpers** in `lib/rightsIssuePermissions.ts` aligned with backend `RightsIssuePermissionCodes` (e.g. `rightsissue:admin:rights-issues:rights-issue:read:any`, `create:any`, `approve:any`).

**Reusable for Admin Frontend:** **Permission-gated navigation**, **IAM proxy route**, **unified API response** shape (`ApiResponse` with `code`, `errors`, `pagination`), **Excel/PDF** tooling (deps: `xlsx`, `jspdf`).

**Gaps for platform admin:** **Domain-specific** to rights issue; **session model differs** from investor-portal (localStorage vs cookies) — **inconsistent** across apps today.

---

### admin (`Frontend/admin`) — condition & relevance

- **Package:** `metronic-react-starter-kit` Next **16**, large dependency set (`@tanstack/react-table`, charts, etc.).
- **Content:** Hundreds of files under `components/layouts/layout-*/` and `app/(layouts)/` — **layout demos**, `app/api/health/route.ts` only for health.
- **Verdict:** **UI shell / design system starter**, **not** connected to DSE IAM or gateway. **Relevance:** Visual/layout **reference only** for a new Admin UI — **do not reuse as application architecture**.

### admin-source (`Frontend/admin-source`) — condition & relevance

- **Package:** `metronic-nextjs` + **Prisma** + **NextAuth** + **AWS S3** + demo `app/api/user-management/*` routes.
- **Verdict:** **Full-featured product demo** (store, user-management CRUD) with **its own auth model**, not IAM. **Relevance:** **Patterns** (React Query, form dialogs) — **not** production auth or API integration for this platform.

---

## D. Service interaction map

### Authentication flow

1. Browser → **Next.js proxy** (`investor-portal` / `rightsissue`) or directly → **Gateway** `10102`.
2. **IAM** at `/iam/api` (gateway-forwarded) issues **cookies** and/or tokens; resource servers validate JWT via **JWKS** (`/api/v1/auth/jwks` on IAM or via gateway issuer).
3. **IAM token audience** lists multiple services — each service may enforce **audience** (ihs documents optional audience bypass for gateway tokens).

### Investor / broker / staff flows

- **Investor/broker:** `investor-portal` → gateway → `core`, `oms`, `onboarding`, `ipo`, `nida`, `ihs`, `rightsissue`.
- **Staff (rights issue):** `rightsissue` UI → gateway → `rights-issue-module` + IAM; permissions enforced in UI and must match IAM.
- **Platform admin:** **No dedicated frontend** in scope; capabilities would combine **IAM admin APIs**, **gateway route management**, **reconciliation/reporting**, **fix-middleware** ops APIs.

### Backend-to-backend communication

- **Kafka** is the primary **async bus** (OMS ↔ FIX ↔ notification ↔ ihs ↔ IPO/rightsissue events).
- **HTTP** used for **onboarding → trading-core** fetch, **GePG**-style callbacks (IPO), **ATS** web service in trading-core.

### Gateway / proxy usage

- Gateway **`DynamicRouteBuilder`** registers **per-route** Camel routes from DB — **service-first** paths like `/reporting/api/v1/...` (see gateway `application.properties` and `DynamicRouteBuilder` comments).
- Frontends **must** align with **path prefixes** registered in gateway DB for each environment.

### Shared domain ownership

- **Investor identity:** IAM + onboarding + trading-core **investor trading account**.
- **Positions:** **ihs** (Kafka-driven), reconciled against exchange/CSD via **reconciliation-service**.
- **Orders:** **OMS** (system of record for orders/events), **fix-middleware** (exchange protocol).

### Duplication / unclear boundaries

- **Onboarding** uses `ddl-auto=update` while others use **Flyway** — **schema ownership risk**.
- **fix-middleware** and **onboarding** both default to **port 10105** in properties — **local port collision** if both run with defaults.
- **Rightsissue frontend** uses **localStorage** session vs **investor-portal** **cookies** — **two security UX models** for staff vs retail.

---

## E. Admin Frontend planning insights

### Admin capabilities already in backends

- **IAM:** Users, roles, permissions (JWT claims); cookie and token endpoints.
- **Rights issue:** Full corporate-action admin + help CMS + IAM permission codes.
- **Reconciliation / reporting:** Ops config API, multi-source reads.
- **Gateway:** Route lifecycle, audit, rate limits (investor dashboard controller — possible ops hooks).
- **FIX / trading-core / OMS:** Operational and market **controllers** (fix-middleware has many `*Controller` classes).

### Reusable from current frontends

- **investor-portal:** Next.js **proxy pattern**, cookie session, middleware route protection, `attachAccessTokenHeader` server helper pattern.
- **rightsissue:** **Permission-based UI**, admin **reporting** pages structure, IAM **BFF route** for token exchange.

### Should not reuse blindly

- **admin / admin-source** app shells — wrong auth and data layers.
- **rightsissue** localStorage session model for a **unified admin** — prefer **one** IAM pattern (likely cookie + gateway like investor-portal).

### Likely admin modules/pages

1. **Identity & access** (IAM) — users, roles, permission assignment, audit.
2. **Gateway** — route/view config, rate-limit policy (if exposed safely).
3. **Market & FIX** — sessions, securities, schedules, caches (fix-middleware + trading-core).
4. **Orders & trading** — OMS surveillance, exception queues, payment controls.
5. **Corporate actions** — IPO + rights issue (or embed links to specialized UIs).
6. **Reconciliation & reporting** — exception dashboards, config toggles.
7. **Notifications** — template/queue health if APIs exist.
8. **Observability** — optional: chaos tool, Grafana links (out of repo scope).

### Missing APIs / gaps before frontend build

- **Consolidated admin discovery:** No single OpenAPI in repo for “all admin capabilities”; expect **per-service** Swagger (`springdoc` where enabled).
- **Onboarding:** Schema/migration story must be **resolved** before admin CRUD on those tables.
- **Cross-service workflows** (e.g. user disable + active orders) may need **orchestration** or clear **runbooks** — not visible as one API.

### Risks / assumptions

- **Gateway route DB** must contain **correct base paths** for every admin client call in each environment.
- **Permission model:** Admin UI needs a **canonical permission taxonomy** beyond rights-issue (`rightsissue:admin:...`) — likely **IAM migration** work.
- **SQL Server** dependencies for reconciliation/reporting — admin **read-only** access patterns and **credential** handling.

---

## F. Technical risks & cleanup recommendations

| Risk | Evidence | Recommendation |
|------|----------|----------------|
| **Empty OMS path** | `Backend/Trading/oms` has no `src` | Remove or rename folder; document OMS lives in `rev-eng-mtp/projects/order-management-service` |
| **Port conflict** | `fix-middleware` & `onboarding` both `server.port=10105` | Align ports or document profiles |
| **Onboarding DDL** | `ddl-auto=update`, TODO for Flyway | Add Flyway before admin tools write to DB |
| **Notification Flyway off** | `spring.flyway.enabled=false` | Validate schema strategy |
| **Secrets in repo** | msmq redis password, various defaults | Move to env/secrets; never copy into new admin |
| **Duplicate session models** | cookies vs localStorage | Standardize new Admin Frontend on **gateway + IAM cookie** pattern |
| **Stub analytics-service** | Only health + main | Either implement or exclude from architecture diagrams |
| **admin / admin-source** | Template demos | Label as **non-production**; avoid importing Prisma/NextAuth stack into DSE admin |

---

## Architectural patterns in use (for the new Admin Frontend)

- **API Gateway** with **database-driven** Camel routes and **JWT** at edge.
- **Resource servers** per domain with **IAM JWKS**.
- **Kafka**-centric **event collaboration** between OMS, FIX, ihs, IPO/rights.
- **Next.js BFF** proxies for **browser** apps to avoid exposing tokens and to **attach** `Authorization` server-side (`attachAccessTokenHeader` pattern in both frontends).

---

## Disclaimer

This document is **evidence-limited to repository contents**; deployment topology (Kubernetes services, exact gateway route rows) should be validated against your **live gateway database** and **IAM permission matrix** before locking Admin UI scope.
