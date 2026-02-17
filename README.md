# Viken Chaos Monkey

Enterprise-grade chaos engineering platform for microservices resilience testing.

## Security Defaults

Chaos endpoints now require HTTP Basic auth with RBAC. Override defaults with environment variables in `application.properties`:

- `chaos-viewer / change-me-viewer` (read-only status/experiments)
- `chaos-operator / change-me-operator` (trigger/abort experiments)
- `chaos-admin / change-me-admin` (kill switch controls)
- `chaos-auditor / change-me-auditor` (read-only auditing access)

## Quick Start

```bash
# Build
./gradlew build

# Run (chaos disabled by default)
./gradlew bootRun

# Run with dev profile (chaos enabled, simulated adapter)
./gradlew bootRun --args='--spring.profiles.active=dev'

# If port 8080 is in use, override:
./gradlew bootRun --args='--spring.profiles.active=dev --server.port=8082'
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/chaos/experiments` | Trigger chaos experiment |
| GET | `/api/chaos/experiments` | List experiments |
| GET | `/api/chaos/experiments/{id}` | Get experiment status |
| DELETE | `/api/chaos/experiments/{id}` | Abort experiment |
| POST | `/api/chaos/emergency/disable` | Activate kill switch |
| POST | `/api/chaos/emergency/enable` | Deactivate kill switch |
| GET | `/api/chaos/status` | Current chaos status |

## Example: Trigger Chaos

```bash
curl -X POST http://localhost:8080/api/chaos/experiments \
  -u chaos-operator:change-me-operator \
  -H "Content-Type: application/json" \
  -d '{
    "type": "pod-kill",
    "targetService": "order-service",
    "namespace": "default",
    "params": {"blastRadius": "5"}
  }'
```

## Configuration

See `application.properties` and `CHAOS_MONKEY_ENTERPRISE_PLAN.md` for full configuration options.

## Project Structure

- `common` - DTOs, response format, correlation ID
- `modules/chaos/api` - REST controller
- `modules/chaos/core` - Orchestration engine
- `modules/chaos/safety` - Kill switch, blast radius, exclusions
- `modules/chaos/scheduler` - Scheduled experiments
- `modules/chaos/adapters` - K8s (when enabled), simulated adapters
- `modules/chaos/audit` - Prometheus metrics, async audit logging

## Metrics (Prometheus)

- `chaos_experiments_total` - Experiments by type, target, status
- `chaos_experiments_failed_total` - Failed experiments
- `chaos_experiments_active` - Currently running experiments
- `chaos_experiment_duration_seconds` - Experiment duration histogram
- `chaos_kill_switch_active` - 1 if kill switch on

Scrape from `/actuator/prometheus`
