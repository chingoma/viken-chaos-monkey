# Viken Chaos Monkey – Enterprise Production Plan

**Version:** 1.0  
**Date:** 14 Feb 2026  
**Scope:** Microservices Chaos Engineering Platform  
**Alignment:** Viken Chaos Monkey Architecture + Enterprise Project Setup Guidelines

---

## 1. Executive Summary

This plan defines a **robust, enterprise-grade, production-ready** chaos engineering platform for microservices. It integrates the Viken Chaos Monkey architecture with your enterprise standards (ISO-aligned, modular monolith, config-service, RBAC, audit trails) to deliver safe, observable, and compliant chaos experiments.

---

## 2. Strategic Goals

| Goal | Description |
|------|-------------|
| **Resilience Validation** | Identify weaknesses before production failures |
| **Automated Chaos** | Safe, scheduled, and on-demand failure injection |
| **Observability** | Full traceability, metrics, and audit compliance |
| **Safety First** | Kill switch, canary, feature flags, blast radius control |
| **Enterprise Compliance** | ISO 27001, OWASP, audit trails, RBAC |

---

## 3. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    VIKEN CHAOS MONKEY (Spring Boot Service)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐ │
│  │ REST API     │  │ Scheduler     │  │ Experiments  │  │ Safety Engine   │ │
│  │ (RBAC)       │  │ (Cron/Quartz)│  │ Repository   │  │ (Kill Switch)   │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────────┬────────┘ │
│         │                 │                 │                    │          │
│         └─────────────────┴────────┬────────┴────────────────────┘          │
│                                    ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Chaos Orchestration Engine                          │  │
│  │  • Experiment validation  • Blast radius  • Approval workflow          │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                       │
│  ┌───────────────────────────────────┼───────────────────────────────────┐  │
│  │              Failure Injection Adapters                               │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │ Kubernetes  │ │ HTTP/Latency│ │ Database    │ │ Message Broker  │  │  │
│  │  │ (Pod Kill)  │ │ (Fault)     │ │ (Slow/Error)│ │ (DLQ/Throttle)  │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
└────────────────────────────────────────┼──────────────────────────────────────┘
                                         │
         ┌───────────────────────────────┼───────────────────────────────┐
         ▼                               ▼                               ▼
┌─────────────────┐            ┌─────────────────┐            ┌─────────────────┐
│ Microservices   │            │ Kubernetes API  │            │ Observability   │
│ (Order, Payment │            │ (Deployments,   │            │ Prometheus      │
│  Inventory...)  │            │  Pods, Services)│            │ Grafana / ELK   │
└─────────────────┘            └─────────────────┘            └─────────────────┘
```

---

## 4. Project Structure (Modular Monolith)

Align with enterprise guidelines: modular, extractable, no circular dependencies.

```
viken.chaos.monkey
├── common                    # Shared utilities, DTOs, response codes
├── security                  # RBAC, OAuth2, audit context
├── infrastructure            # Config, Redis, DB, Camel
├── modules
│   ├── chaos-api             # REST controllers, request validation
│   ├── chaos-core            # Orchestration, experiment engine
│   ├── chaos-scheduler       # Cron/Quartz scheduling
│   ├── chaos-adapters        # K8s, HTTP, DB, MQ adapters
│   ├── chaos-safety          # Kill switch, canary, blast radius
│   └── chaos-audit           # Experiment audit, SIEM events
```

---

## 5. Chaos Experiment Types (Multi-Level Failure Injection)

| Level | Type | Description | Target |
|-------|------|-------------|--------|
| **Infrastructure** | `pod-kill` | Terminate random pod | Kubernetes Deployment |
| **Infrastructure** | `pod-cpu-stress` | CPU throttling | Kubernetes Pod |
| **Infrastructure** | `pod-memory-pressure` | Memory limit simulation | Kubernetes Pod |
| **Infrastructure** | `network-latency` | Inject network delay | Service mesh / Pod |
| **Infrastructure** | `network-partition` | Simulate network split | Service pair |
| **Service** | `http-latency` | Add response delay | HTTP endpoint |
| **Service** | `http-error` | Return 5xx/4xx | HTTP endpoint |
| **Service** | `http-timeout` | Simulate timeout | HTTP client |
| **Database** | `db-slow-query` | Add query delay | PostgreSQL |
| **Database** | `db-connection-exhaustion` | Exhaust connection pool | DataSource |
| **Message** | `mq-delay` | Delay message delivery | Kafka/RabbitMQ |
| **Message** | `mq-dlq-inject` | Force messages to DLQ | Consumer |

---

## 6. Safety & Guardrails (Non-Negotiable)

### 6.1 Kill Switch

- **Global kill switch** via config-service or environment variable
- When enabled: all chaos experiments immediately halted
- Must be persisted and audited
- REST endpoint: `POST /api/chaos/emergency/disable`

### 6.2 Blast Radius Control

- **Max percentage** of target pods/instances per experiment
- **Exclusion lists** for critical services (e.g. payment, auth)
- **Time windows** (e.g. no chaos during business hours)
- **Canary mode**: only 1 pod/instance per experiment initially

### 6.3 Feature Flags

- Per experiment type enabled/disabled via config-service
- Per environment: no chaos in production by default

### 6.4 Approval Workflow (Optional for Enterprise)

- Experiments above certain blast radius require approval
- Audit trail of who approved/rejected

---

## 7. API Design (Enterprise Standards)

### 7.1 Standard Response Format

All chaos API responses follow:

```json
{
  "code": "00",
  "data": {},
  "timestamp": "2026-02-14T10:00:00+03:00",
  "errors": [],
  "requestId": "uuid"
}
```

### 7.2 API Endpoints

| Method | Path | Description |
|--------|------|--------------|
| `POST` | `/api/chaos/experiments` | Create/trigger experiment |
| `GET` | `/api/chaos/experiments` | List experiments |
| `GET` | `/api/chaos/experiments/{id}` | Get experiment status |
| `DELETE` | `/api/chaos/experiments/{id}` | Abort experiment |
| `POST` | `/api/chaos/emergency/disable` | Global kill switch |
| `POST` | `/api/chaos/emergency/enable` | Re-enable chaos |
| `GET` | `/api/chaos/status` | Current chaos status |

### 7.3 Experiment Request Schema

```json
{
  "type": "pod-kill",
  "targetService": "order-service",
  "namespace": "production",
  "params": {
    "blastRadius": "5",
    "duration": "5m"
  }
}
```

---

## 8. Configuration Management

### 8.1 Policy

- **.properties only** (no YAML)
- **Config-service** for business configs (rate limits, experiment types)
- **Environment variables** for secrets and infra URLs

### 8.2 Required Properties

```properties
# Chaos Monkey Core
chaos.enabled=false
chaos.killSwitch.active=false
chaos.scheduler.interval.ms=3600000
chaos.scheduler.cron=0 0 2 * * ?

# Kubernetes (when adapter enabled)
chaos.k8s.enabled=false
chaos.k8s.namespace=default
chaos.k8s.inCluster=true

# Blast Radius
chaos.blastRadius.maxPercent=10
chaos.blastRadius.excludedServices=payment-service,auth-service

# Timezone (MANDATORY)

user.timezone=Africa/Dar_es_Salaam
spring.jackson.time-zone=Africa/Dar_es_Salaam
```

---

## 9. Security & RBAC

### 9.1 Authentication

- OAuth2 / OpenID Connect
- External Authorization Server

### 9.2 Roles

| Role | Permissions |
|------|-------------|
| `chaos-viewer` | Read experiments, status |
| `chaos-operator` | Trigger experiments (within blast radius) |
| `chaos-admin` | Kill switch, full control |
| `chaos-auditor` | Read-only audit logs |

### 9.3 Audit

- Every experiment trigger/abort logged
- Direction: INBOUND (all chaos API calls)
- Persisted to `request_audit_log` (direction-aware schema)
- SIEM topics: `audit.chaos.experiments`, `audit.chaos.emergency`

---

## 10. Observability

### 10.1 Metrics (Prometheus)

| Metric | Type | Description |
|--------|------|-------------|
| `chaos_experiments_total` | Counter | Experiments executed |
| `chaos_experiments_active` | Gauge | Currently running |
| `chaos_experiments_failed` | Counter | Failed injections |
| `chaos_kill_switch_active` | Gauge | 1 if disabled |
| `chaos_experiment_duration_seconds` | Histogram | Duration per type |

### 10.2 Logging

- **Structured JSON** only
- **Correlation ID** propagated
- **OpenTelemetry** distributed tracing

### 10.3 Dashboards (Grafana)

- Chaos experiment history
- Blast radius over time
- Kill switch status
- Failed experiment alerts

---

## 11. Integration Standards (Apache Camel)

For outbound calls (e.g. triggering faults in remote services):

- **Rate limiting** per target service
- **Circuit breakers** (Resilience4j)
- **Timeouts** and retries with backoff
- Configs from config-service

---

## 12. Transaction & Layered Architecture

Per enterprise guidelines:

```
ChaosController
ChaosService (interface)
ChaosServiceImpl
ChaosTransactionService (@Transactional)  # Only here
ChaosExperimentRepository
```

- **No @Transactional** in service implementations
- Dedicated transaction proxy for persistence

---

## 13. Database & Persistence

### 13.1 Tables

| Table | Purpose |
|-------|---------|
| `chaos_experiment` | Experiment definitions and results |
| `chaos_experiment_audit` | Immutable audit trail |
| `chaos_safety_config` | Blast radius, exclusions |

### 13.2 Primary Keys

- **UUIDv7** for all primary keys and public identifiers
- Time-ordered for index efficiency

### 13.3 Partitioning

- `chaos_experiment_audit` partitioned by `created_at` (monthly)

---

## 14. Kubernetes Deployment

### 14.1 Deployment

- Containerized with Docker
- Kubernetes Deployment
- ConfigMaps for non-sensitive config
- Secrets (Vault/KMS) for K8s API tokens

### 14.2 Probes

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`

### 14.3 Resource Limits

- Mandatory CPU/memory limits
- Horizontal scaling only (stateless)

---

## 15. CI/CD & Quality Gates

- Static code analysis (SonarQube, Checkstyle, PMD)
- Dependency scanning
- Container scanning
- ArchUnit: module boundaries, no @Transactional in services
- Unit + integration tests
- **Staging chaos tests** before production rollout

---

## 16. Implementation Phases

### Phase 1: Foundation (Weeks 1–2)

- [ ] Spring Boot project setup (modular structure)
- [ ] ChaosController, ChaosService, ChaosRequest DTO
- [ ] Config-service integration for chaos.enabled
- [ ] Standard response format
- [ ] Basic REST API for manual injection

### Phase 2: Safety & Scheduling (Weeks 3–4)

- [ ] Kill switch implementation
- [ ] Scheduler (Cron/Quartz)
- [ ] Blast radius and exclusion lists
- [ ] Feature flags per experiment type

### Phase 3: Adapters (Weeks 5–7)

- [x] Simulated adapter (all types, for dev/testing)
- [ ] Kubernetes adapter (pod-kill) - add `io.kubernetes:client-java` when K8s available
- [ ] HTTP latency/error adapter
- [ ] Database adapter (optional)
- [ ] Adapter interface for extensibility

### Phase 4: Observability & Audit (Weeks 8–9)

- [x] Prometheus metrics (chaos_experiments_total, chaos_kill_switch_active, etc.)
- [ ] Grafana dashboards
- [x] Chaos experiment audit logging (async, direction-aware)
- [ ] SIEM event publishing (Kafka/Redis Streams)

### Phase 5: Production Hardening (Weeks 10–12)

- [ ] RBAC integration
- [ ] Rate limiting on chaos API
- [ ] Staging validation
- [ ] Runbooks and documentation

---

## 17. Production Go/No-Go Checklist

### Functional

- [ ] Kill switch tested and verified
- [ ] Blast radius enforced
- [ ] All experiment types tested in staging
- [ ] Scheduler respects time windows

### Security

- [ ] RBAC enforced
- [ ] Secrets externalized
- [ ] Audit logs persisted with direction

### Observability

- [ ] Metrics exposed to Prometheus
- [ ] Dashboards configured
- [ ] Alerts for failed experiments

### Compliance

- [ ] Audit retention configured
- [ ] SIEM integration active
- [ ] Architecture Decision Records documented

---

## 18. Risk Matrix

| Risk | Mitigation |
|------|------------|
| Chaos in production during peak | Time windows, blast radius, kill switch |
| Wrong target service | Exclusion lists, validation, dry-run mode |
| K8s API misuse | Least-privilege RBAC, namespace restrictions |
| Audit data loss | Tiered storage, async logging, no sync DB on request |

---

## 19. Appendix: Reference Failure Modes

| Component | Failure | Behavior |
|-----------|---------|----------|
| Config-service | Down | Use cached config, fail-safe (disable chaos) |
| Kubernetes API | Unavailable | Log, retry, do not block scheduler |
| Redis | Down | Fail-open for rate limit (hard cap) |
| Audit DB | Slow | Async queue, backpressure, never block |

---

## 20. Document Control

- **Author**: Kelvin Chingoma
- **Next Review**: 14 Feb 2027
- **Related**: viken_chaos_monkey_pdf_ready.md, project-setup-guideline.md
