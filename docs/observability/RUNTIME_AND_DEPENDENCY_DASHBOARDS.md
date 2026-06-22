# Runtime and Dependency Dashboard Specifications

This document defines implementation-level panel groups and drill-downs for runtime and dependency dashboards.

## 1) Kubernetes workload dashboard

Dashboard: `[L3] K8s Workload - Are Deployments Healthy?`

### Purpose
- Detect availability mismatches and rollout failures before customer impact widens.

### Panel groups
1. Deployment desired vs available replicas
2. Unavailable replicas and rollout progress
3. HPA desired vs current replicas
4. Namespace CPU/memory request vs usage
5. Readiness/liveness failures
6. Pod restart rates
7. Pending pods by scheduling reason

### Drill-down
- to pod runtime dashboard with `environment`, `cluster`, `namespace`, `service`, `pod`
- to node infra dashboard with `node`

## 2) Pod/container runtime dashboard

Dashboard: `[L3] K8s Pod Runtime - Which Pods Are Unstable?`

### Purpose
- Isolate pod-level resource and lifecycle failures.

### Panel groups
1. CPU usage vs limits
2. CPU throttling ratio
3. Memory working set vs limits
4. OOM kill indicators
5. Restart trend and termination reason
6. Network and filesystem error counters (if available)

### Drill-down
- to service logs scoped by `pod`
- to tracing dashboard scoped by `service`

## 3) Node/infrastructure dashboard

Dashboard: `[L3] K8s Node Infra - Is Cluster Capacity Safe?`

### Purpose
- Surface node pressure and cluster-level bottlenecks.

### Panel groups
1. Node CPU/memory usage and saturation
2. Disk usage and I/O pressure
3. Network errors and drops
4. Node conditions (`Ready`, `MemoryPressure`, `DiskPressure`, `PIDPressure`)
5. Pod density and allocatable pressure
6. Control-plane or DNS health signals

## 4) JVM/Spring runtime dashboard

Dashboard: `[L3] Service <service> JVM - Is Runtime Saturated?`

### Spring Boot metrics to prioritize
- `http_server_requests_seconds_*`
- `http_client_requests_seconds_*`
- `jvm_memory_*`
- `jvm_gc_pause_seconds_*`
- `jvm_threads_*`
- `process_cpu_usage`
- `system_cpu_usage`
- `hikaricp_connections_*`
- executor metrics
- resilience metrics from Resilience4j/Micrometer binders

### Panel groups
1. HTTP server RED
2. Outbound HTTP client latency/errors
3. Heap/non-heap and allocation trends
4. GC pause and GC pressure
5. thread states and blocked/waiting
6. executor queue depth and rejection counts
7. DB pool active/idle/pending/timeouts
8. resilience controls (retry/circuit/time limiter/bulkhead)

## 5) Database dashboard

Dashboard: `[L3] Database - Are Queries and Pools Healthy?`

### Panel groups
1. Query throughput and p95/p99 latency
2. Active sessions/connections and wait
3. lock waits and deadlocks
4. replication lag and replica health
5. DB timeout and error rates

## 6) Cache dashboard

Dashboard: `[L3] Cache - Are Hit Ratio and Latency Stable?`

### Panel groups
1. Cache hit/miss ratio and volume
2. Command latency and timeout rate
3. Memory usage and fragmentation
4. evictions and blocked clients
5. connection pressure

## 7) Messaging dashboard (Kafka/RabbitMQ)

Dashboard: `[L3] Messaging - Is Lag or DLQ Growing?`

### Panel groups
1. Publish vs consume rates
2. Consumer lag by group/topic/partition
3. Failed handlers and retry volume
4. DLQ growth trend
5. Handler latency distribution
6. Poison message indicators

### Messaging watch points
- lag growth rate exceeding catch-up capacity
- DLQ slope > 0 for sustained windows
- repeating same failure signature over same key/topic/queue

## 8) Drill-down and ownership conventions

Each dashboard must provide:
- link to owning team runbook
- link to related service health dashboard
- link to logs and traces where applicable

Each panel group should answer one operational question:
- `what changed?`
- `what is failing?`
- `where is bottleneck?`
- `who owns mitigation?`
