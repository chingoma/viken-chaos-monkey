# Telemetry Contract for Production Observability

This contract defines the minimum telemetry standards required for reliable incident response, troubleshooting, and SLO governance across `dev`, `sit`, `uat`, and `prod`.

## 1) Label and Attribute Standards

Use bounded, stable labels only.

### Mandatory metric labels
- `environment`: `dev|sit|uat|prod`
- `cluster`: Kubernetes cluster identifier
- `namespace`: Kubernetes namespace
- `service`: canonical service name
- `team`: owner team name
- `version`: deploy artifact version (for release impact)
- `instance`: pod or instance ID (bounded)

### Request labels (HTTP/gRPC)
- `method`: `GET|POST|...`
- `route`: templated route (`/orders/{id}`), never raw URL
- `status` and `status_class`: `2xx|4xx|5xx`

### Messaging labels
- `topic` or `queue`
- `consumer_group` (Kafka)
- `handler`: consumer handler name (bounded)
- `outcome`: `success|retry|dlq|failure`

### Prohibited high-cardinality labels
- `userId`, `requestId`, `traceId`, `sessionId`, raw query strings
- raw path fragments (`/orders/12345`) instead of route templates

## 2) Logs Contract (Loki)

Emit JSON logs with these minimum fields:
- `timestamp`
- `severity`
- `message`
- `service`
- `team`
- `environment`
- `cluster`
- `namespace`
- `pod`
- `version`
- `trace_id`
- `span_id`
- `error_code` (when applicable)
- `runbook_url` (for known operational failures)

### Logging requirements
- Include `trace_id` and `span_id` for every request path that emits logs.
- Log exceptions with stable `error_code` to support grouping.
- Use structured key/value fields, not free-form multiline text for key data.

## 3) Trace Contract (Tempo/Jaeger via OTel)

Use OpenTelemetry semantic conventions consistently:
- `service.name`
- `deployment.environment`
- `k8s.namespace.name`
- `k8s.pod.name`
- `http.method`
- `http.route` (templated)
- `http.status_code`
- `db.system`, `db.operation`, `db.name`
- `messaging.system`, `messaging.destination.name`, `messaging.operation`

### Span design standards
- Root span per incoming request or message.
- Child spans for:
  - outbound HTTP calls
  - DB queries
  - cache operations
  - message publish/consume handlers
- Record exceptions on spans with canonical status and error attributes.

## 4) Correlation Standards: Metrics -> Logs -> Traces

### Grafana link behavior
- Metrics panel data link to Loki:
  - filter by `service`, `namespace`, `pod`, and selected time range
- Loki log line data link to Tempo:
  - open trace by `trace_id`
- Metrics exemplars:
  - enable exemplars on request latency histograms where backend supports it

### Required dashboard drill-down chain
1. L1/L2 overview panel
2. L3 service panel
3. L4 endpoint/dependency panel
4. Loki logs scoped to failing service/pod/time
5. Tempo trace for causal path
6. runbook link for remediation steps

## 5) Spring Boot + Micrometer Instrumentation Minimums

Expose and retain these metrics:
- `http_server_requests_seconds_*`
- `http_client_requests_seconds_*` (or equivalent)
- `jvm_memory_*`
- `jvm_gc_pause_seconds_*`
- `jvm_threads_*`
- `process_cpu_usage`
- `system_cpu_usage`
- `hikaricp_connections_*`
- executor metrics (`executor_active_threads`, `executor_queued_tasks`, rejections)
- resilience metrics (retry, circuit breaker, timeout, bulkhead) if Resilience4j is used

## 6) Kubernetes Telemetry Minimums

Required exporters/metrics sources:
- kube-state-metrics
- cAdvisor/container metrics
- node exporter
- ingress/gateway metrics

Required dimensions:
- `cluster`, `namespace`, `pod`, `container`, `node`, `workload`

## 7) Ownership and Runbook Convention

Every production service must declare:
- `owner_team`
- `service_tier` (critical, high, medium)
- `pager_rotation`
- `runbook_url`

Every alert and triage dashboard must include owner and runbook context.

Runbook path convention:
- `/runbooks/<domain>/<service>/<scenario>`

## 8) Data Retention and Sampling Guidance

- Metrics retention:
  - high-resolution for triage window (for example, 15s-1m scrape over 7-14 days)
  - downsampled retention for trends/capacity
- Logs retention:
  - longer in `prod` for audit/compliance where required
- Trace retention:
  - retain error traces and slow traces at higher probability
  - use head/tail sampling strategy with bias toward failures and high latency

## 9) Validation Checklist (Go-Live)

- Service emits required labels without cardinality explosions.
- Logs include `trace_id` and `span_id`.
- Traces contain dependency spans (DB, outbound HTTP, messaging).
- Grafana links work both directions (metrics->logs, logs->traces).
- Service dashboard includes runbook and owner metadata.
- Deployment annotations visible for release impact analysis.
