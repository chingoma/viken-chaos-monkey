# Kubernetes RBAC — Viken Chaos Monkey

## Overview

The chaos service requires **minimal** Kubernetes permissions: list and delete pods in a dedicated namespace.

## Apply

```bash
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/deployment.yaml
```

## Permissions

| Resource | Verbs | Scope |
|----------|-------|-------|
| pods | get, list, delete | namespace `chaos-engineering` |

## Pod selection

Experiments target pods with label `app={targetService}` (configurable via `chaos_adapter_config`).

## Production safety

- Set `CHAOS_K8S_ENABLED=false` in prod Deployment (default in `k8s/deployment.yaml`)
- Real pod-kill in PROD requires **both** `chaos.k8s.enabled=true` and `CHAOS_K8S_PROD_OVERRIDE=true`
- Use dedicated namespace; never grant cluster-admin

## Gateway registration

Register in Viken Gateway DB:

```sql
INSERT INTO dynamic_routes (base_path, service_url, enabled)
VALUES ('/chaos', 'http://viken-chaos-monkey.chaos-engineering.svc.cluster.local:20000', true);
```

Browser clients should use admin portal BFF at `/chaos/api/chaos/*`.
