# Service Dashboard Template Standard

Use this template for every Spring Boot microservice to guarantee consistent triage workflows.

## Template metadata
- Dashboard title: `[L3] Service <service> - Why Is It Failing?`
- UID: `obs_l3_service_health_<service>`
- Tags:
  - `tier:l3`
  - `class:triage`
  - `service:<service>`
  - `team:<owner_team>`
  - `env-aware`

## Required variables

| Variable | Type | Required | Notes |
|---|---|---|---|
| `environment` | query/custom | yes | `dev|sit|uat|prod` |
| `cluster` | query | yes | Kubernetes cluster |
| `namespace` | query | yes | Namespace filter |
| `service` | query | yes | service label |
| `version` | query | no | release comparison |
| `pod` | query | no | pod drilldown |
| `instance` | query | no | pod instance/IP |
| `endpoint` | query | no | templated `route` only |
| `method` | query | no | HTTP method |
| `status_code` | query | no | status class/detail |

## Row layout

## Row 1: Snapshot
1. `RPS` (last 5m)
2. `Error ratio %` (5xx / total)
3. `p95 latency`
4. `p99 latency`
5. `Saturation score` (composite panel)
6. `Active versions` (table/stat)

## Row 2: RED
1. Traffic trend
2. Error trend (4xx vs 5xx)
3. p50/p95/p99 latency trend
4. Endpoint error ratio (top N)

## Row 3: Saturation
1. CPU usage vs limit
2. CPU throttling ratio
3. Memory working set vs limit
4. Thread/executor saturation
5. DB pool pending/timeout

## Row 4: Dependencies
1. Outbound HTTP latency/error
2. Database latency/error
3. Cache hit ratio/latency
4. Messaging lag and handler latency

## Row 5: Change impact
1. Deployment annotations and version timeline
2. Pre/post latency delta panel
3. Pre/post error ratio delta panel

## Row 6: Drilldowns
1. Logs link (Loki)
2. Traces link (Tempo)
3. Pod runtime dashboard link
4. Runbook URL

## Threshold standards (default watch points)
- Error ratio warning: `> 1%` (service specific override allowed)
- Error ratio critical: `> 3%` sustained
- p95 latency: objective-based, typically `> 1.5x` baseline
- CPU throttling: warning `> 5%`, critical `> 10%` sustained
- Memory pressure: warning `> 85%`, critical `> 95%` of limit
- DB pool pending: warning on growth trend, critical on timeout growth

## Alert-worthy vs dashboard-only

### Alert-worthy
- sustained SLO impact
- sustained high saturation correlated with user impact
- dependency failure crossing resilience controls

### Dashboard-only
- low-volume p99 spikes
- short-lived single-pod anomalies without user impact

## Correlation links standard

### Metrics panel -> Loki
- Data link fields:
  - `service=$service`
  - `namespace=$namespace`
  - `pod=$pod`
  - `time range=dashboard range`

### Loki -> Tempo
- Parse `trace_id` from JSON logs.
- Data link action opens trace by ID in Tempo/Jaeger datasource.

## Ownership/footer convention
- `owner_team: <team>`
- `pager_rotation: <rotation>`
- `runbook_url: /runbooks/<domain>/<service>/<scenario>`
- `last_reviewed: <date>`

## Review checklist
- Variables resolve in every environment.
- Route dimensions use templated paths, not raw URLs.
- Panels are actionable and have units/thresholds.
- Drilldowns to logs/traces/runbook are functional.
- Deployment annotations visible and time-aligned.
