# PromQL Library for Grafana Dashboards

These queries are reference implementations for the dashboard strategy. Adjust metric names to your exporters if needed.

## Variable assumptions
- `$environment`, `$cluster`, `$namespace`, `$service`
- optional: `$pod`, `$instance`, `$endpoint`, `$method`, `$status_code`, `$node`, `$topic`, `$queue`, `$consumer_group`

## 1) Service RED queries

### Request rate
```promql
sum(
  rate(http_server_requests_seconds_count{
    environment="$environment",
    cluster="$cluster",
    namespace="$namespace",
    service="$service"
  }[5m])
)
```

### 5xx error ratio
```promql
sum(rate(http_server_requests_seconds_count{
  environment="$environment",
  service="$service",
  status=~"5.."
}[5m]))
/
sum(rate(http_server_requests_seconds_count{
  environment="$environment",
  service="$service"
}[5m]))
```

### p95 latency
```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(http_server_requests_seconds_bucket{
      environment="$environment",
      service="$service"
    }[5m])
  )
)
```

### Endpoint p99 by route/method
```promql
histogram_quantile(
  0.99,
  sum by (le, route, method) (
    rate(http_server_requests_seconds_bucket{
      environment="$environment",
      service="$service",
      route!~"UNKNOWN|"
    }[5m])
  )
)
```

## 2) Kubernetes workload and pod queries

### Deployment desired replicas
```promql
kube_deployment_spec_replicas{
  namespace="$namespace",
  deployment=~"$service"
}
```

### Deployment available replicas
```promql
kube_deployment_status_replicas_available{
  namespace="$namespace",
  deployment=~"$service"
}
```

### Pod restart increases
```promql
sum by (pod) (
  increase(kube_pod_container_status_restarts_total{
    namespace="$namespace",
    pod=~"$pod"
  }[15m])
)
```

### CPU throttling ratio
```promql
sum(rate(container_cpu_cfs_throttled_periods_total{
  namespace="$namespace",
  pod=~"$pod"
}[5m]))
/
sum(rate(container_cpu_cfs_periods_total{
  namespace="$namespace",
  pod=~"$pod"
}[5m]))
```

### Memory working set vs limit
```promql
sum(container_memory_working_set_bytes{
  namespace="$namespace",
  pod=~"$pod"
})
/
sum(kube_pod_container_resource_limits{
  namespace="$namespace",
  pod=~"$pod",
  resource="memory"
})
```

### HPA desired vs current
```promql
kube_horizontalpodautoscaler_status_desired_replicas{
  namespace="$namespace"
}
```

```promql
kube_horizontalpodautoscaler_status_current_replicas{
  namespace="$namespace"
}
```

## 3) JVM and Spring Boot queries

### Heap utilization ratio
```promql
sum(jvm_memory_used_bytes{
  area="heap",
  service="$service"
})
/
sum(jvm_memory_max_bytes{
  area="heap",
  service="$service"
})
```

### Non-heap utilization ratio
```promql
sum(jvm_memory_used_bytes{
  area="nonheap",
  service="$service"
})
/
sum(jvm_memory_max_bytes{
  area="nonheap",
  service="$service"
})
```

### GC pause p95
```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(jvm_gc_pause_seconds_bucket{
      service="$service"
    }[5m])
  )
)
```

### Live threads
```promql
sum(jvm_threads_live_threads{
  service="$service"
})
```

### Hikari pending connections
```promql
max(hikaricp_connections_pending{
  service="$service"
})
```

### Hikari timeout count increase
```promql
increase(hikaricp_connections_timeout_total{
  service="$service"
}[5m])
```

### Outbound HTTP client error ratio
```promql
sum(rate(http_client_requests_seconds_count{
  service="$service",
  status=~"5.."
}[5m]))
/
sum(rate(http_client_requests_seconds_count{
  service="$service"
}[5m]))
```

## 4) Node and infrastructure queries

### Node CPU usage ratio
```promql
1 - avg by (instance) (
  rate(node_cpu_seconds_total{
    mode="idle"
  }[5m])
)
```

### Node memory usage ratio
```promql
(
  node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes
)
/
node_memory_MemTotal_bytes
```

### Node filesystem usage ratio
```promql
(
  node_filesystem_size_bytes{fstype!~"tmpfs|overlay"}
  -
  node_filesystem_avail_bytes{fstype!~"tmpfs|overlay"}
)
/
node_filesystem_size_bytes{fstype!~"tmpfs|overlay"}
```

### Node not ready
```promql
kube_node_status_condition{
  condition="Ready",
  status="true"
} == 0
```

## 5) Database queries (generic patterns)

### DB request rate (application metric)
```promql
sum(rate(db_client_requests_seconds_count{
  service="$service"
}[5m])) by (db_system, db_name, operation)
```

### DB p95 latency (application metric)
```promql
histogram_quantile(
  0.95,
  sum by (le, db_system, db_name, operation) (
    rate(db_client_requests_seconds_bucket{
      service="$service"
    }[5m])
  )
)
```

### DB errors ratio (application metric)
```promql
sum(rate(db_client_requests_seconds_count{
  service="$service",
  status="error"
}[5m]))
/
sum(rate(db_client_requests_seconds_count{
  service="$service"
}[5m]))
```

## 6) Cache queries (generic)

### Cache hit ratio
```promql
sum(rate(cache_requests_total{
  service="$service",
  outcome="hit"
}[5m]))
/
sum(rate(cache_requests_total{
  service="$service"
}[5m]))
```

### Cache operation p95 latency
```promql
histogram_quantile(
  0.95,
  sum by (le, operation) (
    rate(cache_operation_seconds_bucket{
      service="$service"
    }[5m])
  )
)
```

### Cache evictions
```promql
increase(cache_evictions_total{
  service="$service"
}[15m])
```

## 7) Messaging queries

### Kafka publish rate (app metric)
```promql
sum(rate(app_messaging_publish_total{
  service="$service",
  topic=~"$topic"
}[5m])) by (topic)
```

### Kafka consume rate (app metric)
```promql
sum(rate(app_messaging_consume_total{
  service="$service",
  topic=~"$topic",
  consumer_group=~"$consumer_group"
}[5m])) by (topic, consumer_group)
```

### Kafka lag
```promql
sum(kafka_consumergroup_lag{
  environment="$environment",
  topic=~"$topic",
  consumergroup=~"$consumer_group"
}) by (topic, consumergroup)
```

### DLQ growth
```promql
sum(increase(app_messaging_dlq_messages_total{
  service="$service",
  environment="$environment"
}[15m])) by (queue)
```

### Handler latency p95
```promql
histogram_quantile(
  0.95,
  sum by (le, handler) (
    rate(app_messaging_handler_seconds_bucket{
      service="$service"
    }[5m])
  )
)
```

## 8) Security and auth queries

### Auth success/failure rate
```promql
sum by (outcome) (
  rate(auth_requests_total{
    environment="$environment",
    service=~"$service|gateway|iam"
  }[5m])
)
```

### 401/403 trend by route
```promql
sum by (status, route) (
  rate(http_server_requests_seconds_count{
    service=~"$service|gateway|iam",
    status=~"401|403"
  }[5m])
)
```

### Rate limit rejections
```promql
sum(rate(gateway_rate_limit_rejections_total{
  environment="$environment"
}[5m])) by (route, client)
```

## 9) SLO and burn-rate queries

### 5m burn-rate for 99.9% SLO
```promql
(
  sum(rate(http_server_requests_seconds_count{
    service="$service",
    status=~"5.."
  }[5m]))
  /
  sum(rate(http_server_requests_seconds_count{
    service="$service"
  }[5m]))
)
/
(1 - 0.999)
```

### 1h burn-rate for 99.9% SLO
```promql
(
  sum(rate(http_server_requests_seconds_count{
    service="$service",
    status=~"5.."
  }[1h]))
  /
  sum(rate(http_server_requests_seconds_count{
    service="$service"
  }[1h]))
)
/
(1 - 0.999)
```
