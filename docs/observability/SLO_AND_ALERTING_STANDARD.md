# SLO and Alerting Standard

This standard defines how to implement actionable, low-noise alerting based on SLI/SLO impact rather than raw metric spikes.

## 1) SLI model

For each critical service, define at minimum:
- Availability SLI:
  - `good = non-5xx responses`
  - `total = all requests`
- Latency SLI:
  - proportion of requests below objective threshold
- Critical journey SLI:
  - business flow completion success ratio

Recommended SLO baselines:
- Tier 0 / customer-critical APIs: `99.9%`
- Tier 1 APIs: `99.5%`
- Tier 2 internal services: `99.0%`

## 2) Error budget model

Error budget for SLO target `S` is `1 - S`.

Example:
- SLO = `99.9%`
- budget = `0.1%`

Burn-rate formula:
- `current_error_rate / error_budget`

Use multi-window burn-rate:
- fast window: detect sharp regressions quickly
- slow window: suppress noise and confirm sustained issues

## 3) Paging policy

### Page (urgent)
- Fast + slow burn-rate both breached.
- Customer-impacting critical journey SLI breach.
- Multi-service ingress 5xx failure.

### Ticket (non-urgent)
- Slow-only burn-rate breach.
- Capacity risk without active customer impact.
- Trend anomaly needing proactive remediation.

## 4) Alert metadata requirements

Every alert rule must include labels/annotations:
- labels:
  - `severity`
  - `service`
  - `team`
  - `tier`
  - `pager_rotation`
- annotations:
  - `summary`
  - `description`
  - `runbook_url`
  - `dashboard_uid`
  - `grafana_link`

## 5) Alert-worthy vs dashboard-only

### Alert-worthy
- sustained SLO burn-rate violation
- sustained ingress/gateway systemic failures
- recurrent OOM/crash loop in critical workloads
- DB pool exhaustion risk with user impact
- runaway messaging lag on critical pipelines

### Dashboard-only
- low-volume endpoint volatility
- short-lived micro-spikes
- exploratory long-tail analysis

## 6) Alert routing

Routing defaults:
- `severity=critical` -> page on-call rotation
- `severity=warning` -> team channel + ticket
- `severity=info` -> dashboard-only, no paging

Escalation:
- if unresolved beyond escalation window, notify platform incident channel and engineering manager.

## 7) SLO dashboard requirements

The SLO dashboard must include:
1. SLI current status by service and critical journey
2. budget remaining (`today`, `7d`, `30d`)
3. fast and slow burn-rate per service
4. forecasted exhaustion time
5. release marker overlays

## 8) Release guardrails

For every deployment:
- compare 30m pre vs 30m post for:
  - error ratio
  - p95 latency
- trigger deploy regression alert when guardrail exceeded for sustained window.

## 9) Implementation checklist

- Define critical service tiers and default SLOs.
- Wire SLI recording rules in Prometheus.
- Implement multi-window burn-rate alerts.
- Add alert metadata with runbook and dashboard links.
- Validate routing and escalation path in non-prod.
