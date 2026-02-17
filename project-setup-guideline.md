# Enterprise Spring Boot Project Setup Guidelines

(Modular Monolith | Microservice-Ready | ISO-Aligned)

---

## 1. ARCHITECTURE PRINCIPLES

### Architectural Positioning

* The project is a **monolithic application**
* Internally structured as a **modular system**
* Externally behaves as **one service within a microservice architecture**
* Communicates with other services strictly via REST, messaging, or event streaming
* No shared databases across services

### Core Technology Stack

* Spring Boot (latest LTS)
* Java LTS only (Java 21 preferred)
* PostgreSQL 18 (LTS)
* Redis (LTS)
* Apache Camel (LTS)
* Configuration format: **.properties only** (no YAML)

### Deployment

* Containerized using Docker
* Deployable on Kubernetes
* Stateless by design
* Horizontal scaling only

---

## 2. PROJECT STRUCTURE & MODULARITY

### Modular Monolith Structure

* Each major business capability is a self-contained module
* Modules communicate via interfaces only

```
tz.project
 ├── common
 ├── security
 ├── infrastructure
 ├── modules
 │    ├── reporting
 │    ├── trading
 │    ├── settlement
 │    └── notifications
```

### Rules

* No circular dependencies between modules
* Modules must be extractable into standalone services in the future
* No module accesses another module’s persistence layer directly

---

## 3. APPLICATION DESIGN STANDARDS

### Identity & Primary Keys

* Use UUIDv7 for primary keys and public identifiers
* UUIDs generated at application level
* Time-ordered for index efficiency
* Database sequences must not be exposed externally

### Layered Architecture

Each module must follow:

```
Controller
Service (interface)
ServiceImpl
Transaction Layer
Repository
```

* Controllers contain no business logic
* Services contain business rules
* Repositories handle persistence only

---

## 4. TRANSACTION MANAGEMENT (MANDATORY)

### Rules

* Do NOT use @Transactional in service implementations
* Do NOT rely on self-invocation
* Transactions must be applied via dedicated transactional proxy classes

Example:

```
OrderService
OrderServiceImpl
OrderTransactionService (@Transactional)
```

### Benefits

* Correct Spring proxy behavior
* Clear transaction boundaries
* Easier auditing and debugging

---

## 5. API & RESPONSE STANDARDS

### Standard Response Object

All APIs must return a unified structure:

```json
{
  "code": "00",
  "data": {},
  "timestamp": "2026-01-25T10:00:00Z",
  "errors": [],
  "requestId": "uuid"
}
```

### HTTP Status Code Policy

* HTTP transport layer always returns 200 OK
* Business outcome determined by code in payload
* Internally classify and log 4xx and 5xx errors for monitoring and audit

### Response Code Governance

* Response codes must be centralized, versioned, and managed via config-service
* Applications cache response codes locally
* No hardcoded response messages

---

## 6. CONFIGURATION MANAGEMENT

### Configuration Policy

* .properties files only
* No YAML permitted

### Global Timezone (MANDATORY)

* Entire project must operate on a **single canonical timezone**
* Default timezone: **Africa/Dar_es_Salaam (GMT +3)**

#### Enforcement

* JVM startup parameter:

  ```
  -Duser.timezone=Africa/Dar_es_Salaam
  ```
* Spring configuration:

  ```properties
  spring.jackson.time-zone=Africa/Dar_es_Salaam
  spring.jpa.properties.hibernate.jdbc.time_zone=Africa/Dar_es_Salaam
  ```
* Database timestamps must use `TIMESTAMPTZ`
* No local or system timezone usage permitted

---

### Sources

* Infrastructure configs via environment variables and .properties
* Business configs fetched from config-service and cached locally

### Rules

* No hardcoded values
* No framework defaults relied upon
* Explicit configuration per environment

---

## 7. DATABASE & PERSISTENCE STANDARDS

### Database Strategy

* Support multiple databases (primary, secondary/reporting)
* Separate DataSource and transaction manager per database

### Rules

* Adding or removing databases must require configuration changes only

---

## 8. INTEGRATION STANDARDS (APACHE CAMEL)

* All external integrations must use Apache Camel
* Mandatory features:

  * Rate limiting
  * Circuit breakers (Resilience4j)
  * Timeouts
  * Retries with backoff
* All Camel configs externalized and fetched from config-service
* Support idempotency, DLQ, and replay

---

## 9. SECURITY STANDARDS

### Lifecycle

* Development: security context disabled
* Pre-production & production: security mandatory

### Authentication & Authorization

* OAuth2 / OpenID Connect
* External Authorization Server preferred
* RBAC mandatory, ABAC where required

### Secrets & Encryption

* No secrets in code, git, logs, or images
* Use Vault / KMS / Secret Manager
* TLS 1.2+ mandatory
* Encryption at rest for sensitive data

---

## 10. INBOUND & OUTBOUND TRAFFIC CONTROL, RATE LIMITING & REQUEST AUDITING

### Traffic Direction Classification (MANDATORY)

Every persisted request log **must explicitly indicate traffic direction**:

* `INBOUND` — Requests entering the service from external clients or other services
* `OUTBOUND` — Requests initiated by this service to external systems (REST, SOAP, MQ, FTP, etc.)

This classification is mandatory for:

* Request persistence
* Audit trails
* SIEM correlation
* Regulatory investigations

Direction must be determined at interception level and **never inferred later**.

---

### Rate Limiting (MANDATORY)

* All inbound requests must be rate limited
* Outbound requests must be rate limited per integration
* Configurable:

  * Globally
  * Per route/path
  * Per HTTP method
  * Per client identity (IP, token, clientId)
* Redis-backed implementation required
* Token bucket or sliding window algorithms only
* Rate limit configs managed via config-service
* Violations logged and returned using standard response payload

---

### Request & Response Logging

* Every incoming and outgoing request must be logged
* Must capture:

  * requestId (correlation ID)
  * Direction (INBOUND | OUTBOUND)
  * Timestamp (UTC)
  * Method and path / target system
  * Sanitized headers
  * Masked request and response payloads
  * Response code
  * Processing duration
  * Client or target system identity

### Sensitive Data Handling

* Mandatory masking of PII, PAN, tokens, credentials
* Masking rules configurable and centralized

---

## 11. REQUEST LOG PERSISTENCE, ARCHIVING & PURGING

### Storage Strategy (Tiered)

#### Tier 1 – Hot Storage

* PostgreSQL table (e.g. request_audit_log)
* Retention: 7–30 days
* Indexed for requestId and timestamp
* Used for debugging and incident response

#### Tier 2 – Warm Storage

* Separate database or schema
* Retention: 3–12 months
* Append-only, minimal indexing
* Used for compliance and internal audits

#### Tier 3 – Cold Storage

* Object storage (S3 / GCS / Azure Blob)
* JSONL or Parquet format
* Immutable
* Retention: 5–7 years

### Archiving & Purging

* Scheduled background job moves data Hot → Warm → Cold
* Must be incremental, idempotent, retryable
* Never delete without archive confirmation

### Database Optimization

* Use time-based partitioning on created_at
* Drop partitions instead of deleting rows

### High-Traffic Preferred Model

* Asynchronous logging pipeline using Kafka / Redis Streams
* Logging must never block request processing
* Backpressure handled gracefully

---

## 12. OBSERVABILITY & AUDIT

* Structured JSON logs only
* Correlation ID mandatory
* Distributed tracing via OpenTelemetry
* Centralized logging (ELK / OpenSearch)
* Audit logs immutable and tamper-evident

---

## 13. PERFORMANCE & SCALABILITY

* HikariCP mandatory
* Redis caching strategy required
* Avoid blocking calls
* Async processing where applicable
* Load and stress testing before production

---

## 14. DEVOPS & DELIVERY

* CI/CD must include:

  * Static code analysis
  * Dependency scanning
  * Container scanning
* Kubernetes:

  * Liveness and readiness probes
  * Resource limits mandatory
* Zero-downtime deployments required

---

## 15. GOVERNANCE & COMPLIANCE

* OpenAPI documentation mandatory
* Architecture Decision Records required
* Code reviews mandatory
* Align with ISO 27001, OWASP Top 10, ISO 20022 (where applicable)

---

## 16. GENERAL ENGINEERING RULES

* Avoid hardcoded values and magic numbers
* Avoid framework defaults
* Prefer explicit configuration, defensive programming, and fail-fast behavior

---

## Appendix A: Reference Implementations (Production-Ready)

> These implementations are **reference-grade**: safe defaults, extensible, and aligned with high‑traffic financial systems. They are intended to be copied into real projects with minimal modification.

---

### A1. Global & Per‑Route Rate Limiting

#### Configuration (`application.properties`)

```properties
rate.limit.global.enabled=true
rate.limit.global.requests=1000
rate.limit.global.windowSeconds=60

rate.limit.routes./api/auth/login.requests=5
rate.limit.routes./api/auth/login.windowSeconds=60

rate.limit.routes./api/payments/**.requests=100
rate.limit.routes./api/payments/**.windowSeconds=60
```

#### Redis‑Backed Rate Limiter Service

```java
@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String key, long limit, long windowSeconds) {
        String redisKey = "rate:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        return count != null && count <= limit;
    }
}
```

#### Rate Limiting Filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = request.getRemoteAddr();
        String path = request.getRequestURI();

        if (!rateLimiterService.allowRequest(clientKey + path, 100, 60)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### A2. Request / Response Audit Logging (Non‑Blocking)

#### Audit Entity (Hot Table)

```java
@Entity
@Table(name = "api_request_audit",
       indexes = {
           @Index(name = "idx_audit_created", columnList = "createdAt"),
           @Index(name = "idx_audit_trace", columnList = "traceId")
       })
public class ApiRequestAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String path;
    private int status;

    @Column(length = 4000)
    private String requestBody;

    @Column(length = 4000)
    private String responseBody;

    private String clientIp;
    private String traceId;

    private Instant createdAt = Instant.now();
}
```

#### Async Audit Publisher

```java
@Component
public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(ApiRequestAudit audit) {
        publisher.publishEvent(audit);
    }
}
```

#### Async Audit Listener

```java
@Component
public class AuditEventListener {

    private final ApiRequestAuditRepository repository;

    public AuditEventListener(ApiRequestAuditRepository repository) {
        this.repository = repository;
    }

    @Async
    @EventListener
    public void handle(ApiRequestAudit audit) {
        repository.save(audit);
    }
}
```

#### Async Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(10_000);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        return executor;
    }
}
```

---

### A3. Audit Logging Filter (Full Traceability)

```java
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditEventPublisher publisher;

    public AuditLoggingFilter(AuditEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(req, res);

        ApiRequestAudit audit = new ApiRequestAudit();
        audit.setMethod(req.getMethod());
        audit.setPath(req.getRequestURI());
        audit.setStatus(res.getStatus());
        audit.setClientIp(req.getRemoteAddr());
        audit.setTraceId(MDC.get("traceId"));
        audit.setRequestBody(new String(req.getContentAsByteArray()));
        audit.setResponseBody(new String(res.getContentAsByteArray()));

        publisher.publish(audit);
        res.copyBodyToResponse();
    }
}
```

---

### A4. Data Retention & Archival Strategy

#### Hot → Warm → Cold Lifecycle

| Tier | Storage                     | Retention   | Purpose                      |
| ---- | --------------------------- | ----------- | ---------------------------- |
| Hot  | PostgreSQL (partitioned)    | 7–30 days   | Live queries, investigations |
| Warm | Separate archive schema     | 6–12 months | Compliance access            |
| Cold | Object storage (S3 / MinIO) | 5–7 years   | Regulatory retention         |

#### Scheduled Archival Job

```java
@Scheduled(cron = "0 0 2 * * *")
@Transactional
public void archiveOldAuditData() {
    repository.archiveOlderThan(Instant.now().minus(30, ChronoUnit.DAYS));
}
```

---

### A5. Operational Rules (Mandatory)

* ❌ Never block request threads for logging
* ❌ Never hard‑delete audit data
* ✅ All rate limits configurable via `.properties`
* ✅ Audit failure must **never** fail business requests
* ✅ All tables must be time‑partitioned in high‑volume systems

---

### A6. Compliance Note

This implementation supports:

* PCI‑DSS audit trails
* ISO 27001 logging controls
* SOC 2 evidence requirements
* Financial regulator traceability

---

**This appendix is mandatory for all inbound APIs in the platform.**

---

## APPENDIX C: REDIS LUA ATOMIC RATE LIMITING

### Purpose

Ensure atomic, race-condition-free rate limiting under high concurrency.

### Algorithm

Sliding Window using Redis Lua script.

### Lua Script (Atomic)

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)

if count >= limit then
  return 0
end

redis.call('ZADD', key, now, now)
redis.call('EXPIRE', key, window)
return 1
```

### Guarantees

* Fully atomic
* No race conditions
* Accurate under burst traffic

---

## APPENDIX D: POSTGRESQL PARTITIONING TEMPLATES

### Hot Table (request_audit_log)

```sql
CREATE TABLE request_audit_log (
  id UUID NOT NULL,
  request_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  method TEXT,
  path TEXT,
  status_code TEXT,
  duration_ms BIGINT,
  request_payload JSONB,
  response_payload JSONB
) PARTITION BY RANGE (created_at);
```

### Monthly Partition

```sql
CREATE TABLE request_audit_log_2026_01
PARTITION OF request_audit_log
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

### Drop Strategy

```sql
DROP TABLE request_audit_log_2025_12;
```

---

## APPENDIX E: SIEM & EVENT STREAMING

### Event Streaming

* Every request audit event published asynchronously
* Kafka / Redis Streams preferred

### Topics

* `audit.requests`
* `security.rate_limit`
* `security.anomalies`

### Payload Contract

```json
{
  "eventType": "REQUEST_AUDIT",
  "requestId": "uuid",
  "service": "service-name",
  "timestamp": "UTC",
  "severity": "INFO",
  "payload": {}
}
```

### Consumers

* SIEM
* Fraud engines
* Security analytics

---

## APPENDIX F: FAILURE MODE & RESILIENCE MATRIX

| Component     | Failure | Behavior                   |
| ------------- | ------- | -------------------------- |
| Redis         | Down    | Fail-open with hard cap    |
| DB Hot        | Slow    | Async queue + backpressure |
| Kafka         | Down    | Local disk buffer          |
| Archive Job   | Fails   | Retry, never delete        |
| Masking Rules | Missing | Fail request               |

---

## APPENDIX G: NON-NEGOTIABLE RULES

* Rate limiting must never be bypassed
* Audit logging must never block requests
* No deletes without archive confirmation
* No synchronous DB writes on request thread
* No sensitive data in logs, ever

---

## APPENDIX H: CODE REVIEW ENFORCEMENT CHECKLIST

* [ ] Rate limit applied
* [ ] Correlation ID propagated
* [ ] Async logging used
* [ ] Masking verified
* [ ] Partition strategy respected
* [ ] Archiving job registered
* [ ] Failure modes handled

---

## APPENDIX I: CANONICAL REQUEST AUDIT LOG SCHEMA

### Logical Schema (Direction-Aware)

```sql
CREATE TABLE request_audit_log (
  id UUID NOT NULL,
  request_id UUID NOT NULL,
  direction VARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
  service_name TEXT NOT NULL,
  method TEXT,
  path_or_target TEXT,
  client_identity TEXT,
  status_code TEXT,
  duration_ms BIGINT,
  request_headers JSONB,
  response_headers JSONB,
  request_payload JSONB,
  response_payload JSONB,
  created_at TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (created_at);
```

### Mandatory Indexes

* `(request_id)`
* `(direction, created_at)`
* `(service_name, created_at)`

---

## APPENDIX J: GOLDEN INTERCEPTOR / FILTER ORDERING

### Inbound Request Chain (Strict Order)

1. Correlation ID Filter
2. Timezone Context Initializer
3. Rate Limiter (Inbound)
4. Authentication Filter
5. Authorization Filter
6. Request Audit Capture (Pre)
7. Controller Invocation
8. Response Audit Capture (Post)
9. Async Log Publisher

### Outbound Request Chain

1. Correlation ID Propagation
2. Rate Limiter (Outbound)
3. Circuit Breaker
4. Retry Policy
5. Request Audit Capture
6. Async Log Publisher

Deviation from this order is **not permitted**.

---

## APPENDIX K: SECURITY INCIDENT CLASSIFICATION

### Incident Levels

| Level    | Description         | Examples               |
| -------- | ------------------- | ---------------------- |
| LOW      | Policy violation    | Rate limit exceeded    |
| MEDIUM   | Suspicious behavior | Repeated auth failures |
| HIGH     | Confirmed attack    | Token abuse, scanning  |
| CRITICAL | Breach              | Data exfiltration      |

### Mandatory Actions

* HIGH / CRITICAL events must be:

  * Persisted immediately
  * Streamed to SIEM
  * Alerted

---

## APPENDIX L: PRODUCTION GO / NO-GO CHECKLIST

### Functional

* [ ] All endpoints rate limited
* [ ] Inbound & outbound logs persisted
* [ ] Direction field populated
* [ ] Async logging verified under load

### Security

* [ ] Secrets externalized
* [ ] TLS enforced
* [ ] Masking verified
* [ ] Authorization rules validated

### Performance

* [ ] Load test passed
* [ ] Redis failure tested
* [ ] DB partitioning active

### Compliance

* [ ] Audit retention configured
* [ ] Archival job scheduled
* [ ] SIEM integration active

Deployment without full checklist completion is **forbidden**.

---

## APPENDIX M: GOLDEN PATH (END-TO-END FLOW)

1. Request enters service
2. Correlation ID assigned
3. Timezone enforced (GMT+3)
4. Rate limit checked
5. Security validated
6. Business logic executed
7. Response generated
8. Audit event published async
9. Metrics and traces emitted

This path must remain intact across all modules.

---

# DOCUMENT VERSIONING & AUTHORSHIP

* **Standard Version**: Enterprise Standard v1.0
* **Author**: Kelvin Chingoma
* **Organization**: [Your Company / Department]
* **Date**: 25 Jan 2026
* **Scope**: Modular Monolith Services within Microservice Architecture
* **Reviewed by**: Architecture Review Board / Security Team
* **Next Review**: 25 Jan 2027

## ENFORCEMENT HOOKS

### ArchUnit Rules

* Enforce module boundaries (no cross-module repository access)
* Ensure Service → ServiceImpl → Transaction Layer pattern
* Enforce UUIDv7 usage for primary keys
* No use of @Transactional inside service implementations

### Checkstyle / PMD

* Ban magic numbers / hard-coded values
* Enforce explicit configuration usage
* Mandatory logging of all exceptions
* Mandatory use of centralized response object

### CI/CD Gates

* Static code analysis must pass
* Dependency & container scanning must pass
* Unit, integration, and load tests must pass
* All hooks (ArchUnit, Checkstyle, PMD) must pass before merge to main

