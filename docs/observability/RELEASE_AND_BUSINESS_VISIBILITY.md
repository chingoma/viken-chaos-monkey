# Release and Business Visibility Standard

This standard defines how to detect change-induced regressions quickly and track customer-impacting journey health.

## 1) Release / deployment impact dashboard

Dashboard: `[L2] Release Impact - Did Change Break Reliability?`

## Purpose
- Show immediate before/after impact of deployments on latency, errors, and throughput.
- Enable fast rollback decisions with confidence signals.

## Target audience
- Release managers
- SRE / incident commander
- Service owners

## Required panel groups
1. Deployment markers timeline (service, version, commit, actor)
2. Version distribution and skew by service
3. Before/after p95 latency delta
4. Before/after 5xx ratio delta
5. Canary vs baseline comparison (where used)
6. Rollback indicators and post-rollback recovery trend
7. Change failure signal table (per service)

## Required metrics/signals
- `deployment_events_total` and deploy annotations
- request rate, p95/p99 latency, 5xx ratio by `service` and `version`
- pod restart and readiness failures by `version`
- error-budget burn-rate before and after deployment

## Threshold/watch points
- p95 latency increase > 20% sustained 15m after deploy
- 5xx ratio increase > 1 percentage point sustained 10m
- burn-rate jump > 2x baseline after deploy
- readiness/restart anomalies concentrated in new version

## Alert-worthy vs dashboard-only

### Alert-worthy
- sustained post-deploy error ratio or latency guardrail violation
- rollback required due to objective breach

### Dashboard-only
- small transient post-warmup shifts
- low-volume route fluctuations

## Drill-down path
1. release dashboard row -> service L3 dashboard pre-filtered by version
2. service dashboard -> logs diff by old/new version
3. logs -> traces (`trace_id`) for critical failing requests
4. runbook `deploy-regression` and rollback playbook

## 2) Deployment annotation standard (Grafana)

Each deployment marker should include:
- `service`
- `version`
- `environment`
- `cluster`
- `namespace`
- `change_ticket`
- `commit_sha`
- `deployer`

Annotation text format:
- `deploy service=<service> version=<version> sha=<sha> ticket=<ticket>`

## 3) Business transaction / critical journey dashboard

Dashboard: `[L2] Business Journey - Are Critical Flows Completing?`

## Purpose
- Measure user-visible outcomes across end-to-end business flows.
- Tie technical failures to customer and operational impact.

## Target audience
- product engineering leadership
- support and operations
- SRE and service owners

## Required panel groups
1. Journey throughput (start, completion, failure)
2. Journey success ratio and SLI
3. End-to-end journey p95/p99 latency
4. Stage funnel completion vs abandonment
5. Failure reason taxonomy and trend
6. Dependency contribution map (which service/dependency caused failure)

## Critical journeys to model (example categories)
- investor onboarding
- authentication and session establishment
- funding/payment completion
- order placement and confirmation
- settlement/notification completion

## Journey metric contract

Emit counters/histograms with bounded labels:
- `business_journey_started_total{journey,environment,service}`
- `business_journey_completed_total{journey,environment,service}`
- `business_journey_failed_total{journey,environment,service,reason_code}`
- `business_journey_duration_seconds_bucket{journey,environment}`
- `business_journey_stage_total{journey,stage,outcome}`

Do not include user identifiers as labels.

## Threshold/watch points
- journey success ratio falls below SLO target
- p95 journey latency increases > 25% sustained
- stage abandonment anomaly (statistically unusual increase)

## Alert-worthy vs dashboard-only

### Alert-worthy
- critical journey SLI breach
- sustained abandonment anomaly in critical funnel stage

### Dashboard-only
- low-volume experimental journey variance
- non-critical stage fluctuations

## Drill-down path
1. journey degradation panel -> related services
2. service panels -> endpoint and dependency details
3. logs and traces by journey correlation IDs
4. runbook for customer-impact scenario

## 4) Rollback indicator standard

A release is marked `rollback_candidate=true` if all conditions are met:
1. post-deploy error ratio exceeds baseline guardrail
2. post-deploy latency exceeds baseline guardrail
3. no traffic surge explains the delta

Recovery verification:
- metrics return to baseline after rollback marker
- burn-rate normalizes within expected recovery window
