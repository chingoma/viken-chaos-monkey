# Production-Grade Grafana Dashboard Strategy

This document defines a scalable, low-noise Grafana operating model for Spring Boot microservices on Kubernetes using Prometheus, Loki, and Tempo/Jaeger.

## 1. Overall observability strategy

### 1.1 Operational model
- Use a strict drill-down path: `L1 Platform -> L2 Fleet/Domain -> L3 Service -> L4 Endpoint/Dependency -> Pod -> Logs -> Trace -> Runbook`.
- Treat telemetry systems by purpose:
  - metrics: detection and trend
  - logs: evidence and error context
  - traces: causality and latency decomposition
- Build two dashboard classes:
  - triage dashboards (fast RCA, low cardinality, low noise)
  - analysis dashboards (deeper investigation after impact is contained)

### 1.2 Methods and reliability model
- **RED** for request workloads: rate, errors, duration.
- **USE** for infra/runtime: utilization, saturation, errors.
- **Golden Signals** at each layer: latency, traffic, errors, saturation.
- **SLI/SLO/Error Budget** for paging policy and release safety.

### 1.3 Ownership and support model
- Every dashboard and alert must expose:
  - `owner_team`
  - `service`
  - `tier`
  - `runbook_url`
  - `pager_rotation`
- L1/L2 owned by platform + SRE; L3/L4 owned by service teams with platform guardrails.

### 1.4 Noise and scalability rules
- Prefer percentile and error-ratio panels over raw counters.
- Keep triage dashboards intentionally small (actionable top panels only).
- Never include unbounded labels in Prometheus series.
- Split dashboards by operational question, not by data source.

## 2. Recommended dashboard hierarchy and folder structure

```text
00-Global
  [L1] Platform Global - Are We Healthy?
  [L2] SLO Error Budget - Are We Burning Too Fast?
  [L2] Release Impact - Did Change Break Reliability?

01-Edge
  [L3] API Gateway Edge - Is Traffic Reliable?
  [L3] Auth IAM Gateway - Is Access Healthy and Safe?

02-Service-Catalog
  [L2] Service Catalog - What Is Degraded?
  [L2] Domain <domain> - Which Service Is At Risk?

03-Services/<service-name>
  [L3] Service <service> - Why Is It Failing?
  [L4] Service <service> API - Which Endpoint Regressed?
  [L3] Service <service> JVM - Is Runtime Saturated?
  [L3] Service <service> Dependencies - Is Downstream the Cause?
  [L3] Service <service> Logs - What Failed?
  [L3] Service <service> Tracing - Where Is Latency Spent?

04-Kubernetes
  [L3] K8s Workload - Are Deployments Healthy?
  [L3] K8s Pod Runtime - Which Pods Are Unstable?
  [L3] K8s Node Infra - Is Cluster Capacity Safe?

05-Data-Platforms
  [L3] Database - Are Queries and Pools Healthy?
  [L3] Cache - Are Hit Ratio and Latency Stable?
  [L3] Messaging - Is Lag or DLQ Growing?

06-Business
  [L2] Business Journey - Are Critical Flows Completing?

99-Shared-Library
  reusable panel snippets and variables
```

## 3. Naming convention standard

### 3.1 Dashboard naming
- Format: `[<Tier>] <Scope> - <PrimaryQuestion>`
- Examples:
  - `[L1] Platform Global - Are We Healthy?`
  - `[L2] Service Catalog - What Is Degraded?`
  - `[L3] Service payments - Why Is It Failing?`
  - `[L4] Service payments API - Which Endpoint Regressed?`

### 3.2 UID and tags
- UID: `obs_<tier>_<scope>_<purpose>`
- Tags (mandatory):
  - `tier:l1|l2|l3|l4`
  - `class:triage|analysis`
  - `team:<owner>`
  - `service:<name|shared>`
  - `env-aware`

### 3.3 Runbook and ownership conventions
- Dashboard annotation footer must show:
  - `owner_team`
  - `slack_channel` (or equivalent)
  - `runbook_url`

## 4. Detailed dashboard-by-dashboard design

The sections below define purpose, users, operational questions, panel groups, watch points, alert boundaries, and drill-downs.

## A) Executive / Platform overview
- Purpose: 60-second status for active incident command and leadership.
- Target audience: incident commander, SRE lead, engineering leadership.
- Key metrics: global request rate, global 5xx ratio, p95/p99 latency, SLO burn-rate, cluster pressure.
- Panel groups:
  - global SLO compliance (`today`, `7d`, `30d`)
  - top error budget burners by service/team
  - global RED trends
  - cluster health rollup (CPU/memory/pod pressure)
  - active alerts by severity
  - deployment markers and incident timeline
- Thresholds/watch points:
  - 5xx surge > baseline, p95/p99 shift, burn-rate > `2x`
- Alert-worthy:
  - multi-window burn-rate violations
  - multi-service ingress 5xx surge
- Dashboard-only:
  - long-horizon trends for capacity and reporting
- Drill-down path:
  - service catalog -> affected service -> endpoint -> pod -> logs -> trace -> runbook

## B) Service overview / service catalog health
- Purpose: fleet-level health and ownership-centric triage.
- Audience: SRE, platform, service on-call.
- Key metrics: service availability, service p95, error ratio, burn-rate, restart pressure, version skew.
- Panel groups:
  - service health table (RAG scoring + owner)
  - regressions table (latency/error deltas vs baseline)
  - dependency risk heatmap
  - unhealthy probes/restarts by namespace
  - version skew map
- Watch points:
  - healthy pods but high app errors
  - rollout skew with post-deploy regressions
- Alert-worthy:
  - service-level SLO breach risk
  - sustained error-ratio breach
- Drill-down:
  - one-click to service dashboard with inherited variables

## C) Per-service health dashboard
- Purpose: first-stop triage page for one service.
- Audience: service owners, on-call responders.
- Key metrics: RED + saturation + dependency health + release context.
- Panel groups:
  - summary stats (`RPS`, `error%`, `p50/p95/p99`, saturation score)
  - RED by endpoint group
  - saturation (CPU throttling, memory pressure, thread pools, DB pool)
  - dependencies (outbound HTTP, DB, cache, broker)
  - release markers and version timeline
  - drill-down links to logs/traces/runbook
- Watch points:
  - stable traffic with rising latency
  - endpoint-specific 5xx spikes
  - throttling correlated with latency
- Alert-worthy:
  - sustained SLO-derived error threshold breaches
  - p95/p99 sustained objective breaches
  - dependency failures crossing resilience thresholds

## D) API / endpoint performance dashboard
- Purpose: endpoint hotspot and regression analysis.
- Audience: API engineers and performance owners.
- Key metrics: endpoint rate, endpoint error ratio, p95/p99 by route/method/status.
- Panel groups:
  - endpoint RED table (`route`, `method`, `status_class`)
  - p95/p99 latency per endpoint
  - error ratio and top status codes
  - response/payload size (if instrumented)
  - slow endpoints with release overlay
- Watch points:
  - cardinality: templated route only
  - mission-critical route regressions
- Alert-worthy:
  - endpoint SLI breach on critical APIs

## E) API gateway / ingress dashboard
- Purpose: edge reliability and policy effects.
- Audience: platform/edge team, SRE.
- Key metrics: ingress traffic, 4xx/5xx, p95/p99, rejection/rate-limit counts, upstream failure mapping.
- Panel groups:
  - gateway RED
  - upstream latency/error
  - rate-limit/rejection trend
  - auth failures at edge
  - upstream service blame split
- Watch points:
  - 502/503 spikes
  - sudden 401/403 shifts
  - rejection bursts
- Alert-worthy:
  - edge 5xx affecting multiple services
  - persistent rejection anomalies

## F) Kubernetes workload dashboard
- Purpose: deployment-level availability and scaling health.
- Audience: platform engineers, SRE.
- Key metrics: desired vs available replicas, rollout status, HPA, readiness/liveness, scheduling failures.
- Panel groups:
  - desired/available replicas
  - rollout and unavailable replicas
  - HPA desired/current
  - namespace request vs usage saturation
  - probe failures and restart rates
  - pending pods by reason
- Watch points:
  - readiness degradation before incident blast radius grows
  - pending pods due to scheduling constraints
- Alert-worthy:
  - sustained `desired != available`
  - repeated probe failures and crash loops

## G) Pod / container runtime dashboard
- Purpose: per-pod fault isolation and noisy-neighbor detection.
- Audience: service on-call and platform.
- Key metrics: CPU usage, throttling ratio, memory working set, OOM indicators, restart reason.
- Panel groups:
  - CPU vs limit and throttling
  - memory vs limit and OOM indicators
  - restart/termination reasons
  - container network/filesystem errors (if available)
- Watch points:
  - throttling > 10% sustained
  - memory trend toward OOM
- Alert-worthy:
  - crash loop recurrence
  - OOM in critical production services

## H) Node / infrastructure health dashboard
- Purpose: cluster pressure and infra-level risk.
- Audience: platform/SRE.
- Key metrics: node CPU/memory/disk/network, node conditions, allocatable saturation.
- Panel groups:
  - node resource utilization and saturation
  - node conditions (`Ready`, `MemoryPressure`, `DiskPressure`, `PIDPressure`)
  - pod density and allocatable usage
  - control-plane health signals (if available)
- Alert-worthy:
  - node not ready
  - sustained pressure conditions
  - disk saturation risk

## I) JVM / Spring Boot runtime dashboard
- Purpose: runtime bottleneck detection in Java services.
- Audience: Java developers and SRE.
- Key metrics:
  - HTTP server/client latency and errors
  - heap/non-heap usage
  - GC pause and GC pressure
  - thread states and pool health
  - DB pool utilization
  - resilience control states
- Panel groups:
  - JVM memory and allocation
  - GC pause/frequency/overhead
  - threads and executors
  - Hikari connection pools
  - server/client HTTP RED
  - retries/circuit breakers/timeouts/bulkheads
- Alert-worthy:
  - GC pause inflation with user impact
  - DB pending/timeout growth
  - executor rejection spikes

## J) Database dashboard
- Purpose: DB bottlenecks and durability risk.
- Audience: service owners, DBA, SRE.
- Key metrics: query latency, throughput, connection usage, lock waits, replication lag, error timeouts.
- Panel groups:
  - query latency + throughput
  - pool/session utilization and wait
  - lock waits/deadlocks
  - replication lag
  - DB errors and timeout rates
- Alert-worthy:
  - connection exhaustion
  - lock/deadlock spikes
  - replication lag beyond RPO objective

## K) Cache dashboard
- Purpose: cache efficiency and downstream protection.
- Audience: service owners + platform.
- Key metrics: hit ratio, command latency, evictions, memory pressure, blocked clients.
- Panel groups:
  - hit/miss ratio + request volume
  - command latency and timeout rate
  - memory usage/fragmentation/evictions
  - client and connection pressure
- Alert-worthy:
  - sustained miss spikes with DB pressure correlation
  - elevated cache latency/timeouts

## L) Messaging / Kafka / RabbitMQ dashboard
- Purpose: async throughput and reliability control.
- Audience: service owners, platform messaging team, SRE.
- Key metrics: publish/consume rate, lag, retries, DLQ, handler latency, poison indicators.
- Panel groups:
  - publish vs consume rates
  - consumer lag by group/topic/partition
  - failed processing/retries
  - DLQ growth trend
  - handler latency
  - repeated failure signatures
- Alert-worthy:
  - lag growth that cannot converge
  - sustained DLQ growth
  - handler latency SLI breaches

## M) Logs / exceptions dashboard
- Purpose: rapid error evidence and pattern clustering.
- Audience: on-call responders and developers.
- Key metrics: error log volume by severity/service/version, exception class trends.
- Panel groups:
  - error rate heatmap by service/version
  - top exception classes over time
  - correlated logs with trace links
  - deploy-window error diff
- Alert-worthy:
  - high-severity novel exception spikes
- Dashboard-only:
  - long-tail exception exploration

## N) Distributed tracing / dependency dashboard
- Purpose: dependency causality and latency decomposition.
- Audience: service owners and SRE.
- Key metrics: edge latency, span error ratio, critical path duration, top slow spans.
- Panel groups:
  - service dependency graph
  - top slow spans + erroring spans
  - trace explorer by endpoint/version/status
  - upstream/downstream latency contribution
- Alert-worthy:
  - downstream latency surge propagating to critical user-facing paths

## O) SLO / error budget dashboard
- Purpose: reliability governance and paging guardrail.
- Audience: SRE, engineering management, service owners.
- Key metrics: SLI attainment, error budget remaining, burn-rate fast/slow windows, breach forecast.
- Panel groups:
  - SLI status per critical service/journey
  - budget remaining and burn-rate
  - attainment trend by environment
  - breach forecast and projected exhaustion
- Alert-worthy:
  - multi-window burn-rate conditions

## P) Release / deployment impact dashboard
- Purpose: identify change-induced regressions quickly.
- Audience: release managers, service owners, SRE.
- Key metrics: pre/post error ratio and latency delta, version skew, rollback outcomes.
- Panel groups:
  - deployment annotations and release timeline
  - pre vs post latency
  - pre vs post error ratio
  - canary vs baseline panel
  - rollback signal and recovery panel
- Alert-worthy:
  - post-deploy guardrail failures

## Q) Authentication / IAM / security gateway dashboard
- Purpose: access reliability and abuse signal visibility.
- Audience: IAM/security/platform teams.
- Key metrics: auth success/failure, token validation errors, 401/403, rate-limit and policy rejects.
- Panel groups:
  - auth success/failure trends
  - token validation failures by reason
  - 401/403 by route/client
  - rejection/rate-limit patterns
  - suspicious burst panel (source/client)
- Alert-worthy:
  - auth failure surges
  - validation anomaly spikes
  - unusual rejection patterns

## R) Business transaction / critical user journey dashboard
- Purpose: direct customer-impact observability.
- Audience: product engineering leadership, support, incident command.
- Key metrics: journey start/complete/fail rates, end-to-end latency, abandonment, stage errors.
- Panel groups:
  - journey throughput and success ratio
  - journey p95/p99 latency
  - funnel stage completion/abandonment
  - failure reasons and technical dependency attribution
- Alert-worthy:
  - journey SLI breach
  - abandonment anomalies

## 5. Reusable standard dashboard template

Template name: `Service Health Standard`.

### Required template variables
- `environment`, `cluster`, `namespace`, `service` (required defaults)
- `version`, `pod`, `instance`
- `endpoint`, `method`, `status_code`
- optional: `node`, `topic`, `queue`, `consumer_group`

### Template rows
1. Snapshot (`RPS`, `error%`, `p95`, saturation score, active versions)
2. RED (traffic, errors, latency)
3. Saturation (CPU throttling, memory pressure, threads/executors, DB pools)
4. Dependencies (HTTP clients, DB/cache/messaging)
5. Change impact (deployment markers, pre/post deltas)
6. Drill-down (logs, traces, Kubernetes runtime, runbook)

### Template guardrails
- At most 12 high-value panels in triage mode.
- Use consistent panel titles and units across services.
- Keep per-endpoint and per-pod breakdowns collapsible to avoid clutter.

## 6. Example PromQL queries

See `PROMQL_LIBRARY.md` for full query set. The strategy uses these canonical query patterns:
- RED (`rate`, `error ratio`, percentile latency)
- K8s availability/saturation (`desired vs available`, restarts, throttling, memory pressure)
- JVM internals (heap ratio, GC pause percentile, connection pool pending)
- messaging (lag and DLQ growth)
- auth/security (401/403 and token failures)
- SLO burn-rate

## 7. Alerting guidance

### Alert-worthy signals
- direct or imminent customer-impact
- SLO burn-rate fast and slow window violations
- sustained saturation with error/latency impact
- known release regression guardrail breaches

### Dashboard-only signals
- exploratory trends
- low-volume percentile noise
- non-actionable single-point spikes

### Alert metadata standard
- include `service`, `team`, `severity`, `runbook_url`, `dashboard_uid`, `pager_rotation`.

## 8. Implementation roadmap by priority

1. Foundation (L1/L2/L3 + telemetry correlation + deployment annotations)
2. Runtime and dependency depth (K8s, JVM, DB/cache/messaging)
3. Reliability governance (SLO dashboard + burn-rate paging)
4. Change and business impact (release dashboard + journeys)
5. Ongoing governance (cardinality audits, panel pruning, alert fatigue review)

## 9. Anti-patterns to avoid

- monolithic “everything dashboard”
- averages without percentile visibility
- dashboards with no owner/runbook/deploy markers
- direct alerting on every panel threshold
- mixing environments without strict default filter
- metric label explosions from unbounded identifiers
- no distinction between triage and deep analysis

## 10. Final recommended baseline dashboard pack for immediate rollout

- `[L1] Platform Global - Are We Healthy?`
- `[L2] Service Catalog - What Is Degraded?`
- `[L3] Service <service> - Why Is It Failing?` (from template)
- `[L3] API Gateway Edge - Is Traffic Reliable?`
- `[L3] K8s Workload - Are Deployments Healthy?`
- `[L3] K8s Pod Runtime - Which Pods Are Unstable?`
- `[L3] K8s Node Infra - Is Cluster Capacity Safe?`
- `[L3] Service <service> JVM - Is Runtime Saturated?`
- `[L3] Database - Are Queries and Pools Healthy?`
- `[L3] Cache - Are Hit Ratio and Latency Stable?`
- `[L3] Messaging - Is Lag or DLQ Growing?`
- `[L3] Service <service> Logs - What Failed?`
- `[L3] Service <service> Tracing - Where Is Latency Spent?`
- `[L2] SLO Error Budget - Are We Burning Too Fast?`
- `[L2] Release Impact - Did Change Break Reliability?`
- `[L3] Auth IAM Gateway - Is Access Healthy and Safe?`
- `[L2] Business Journey - Are Critical Flows Completing?`
