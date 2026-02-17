I have prepared a PDF-ready document for Viken Chaos Monkey that includes both the architecture diagram and Spring Boot implementation. To generate the PDF, you can use any Markdown-to-PDF or text-to-PDF tool. Here’s the text you can export:

---

# Viken Chaos Monkey – Enterprise-Grade Chaos Engineering with Spring Boot

## 1. Overview
Viken Chaos Monkey is a production-ready chaos engineering platform for testing resilience in microservices architectures. It provides centralized chaos control, automated failure injection, and observability integration.

**Goals:**
- Identify weaknesses before production failures.
- Automate chaos experiments safely.
- Provide actionable insights.

## 2. Architecture
```
           ┌───────────────────────────┐
           │   Viken Chaos Monkey      │
           │  (Spring Boot Service)    │
           │ ┌───────────────────────┐│
           │ │ Chaos Experiments Repo ││
           │ │ & Scheduling Engine   ││
           │ └─────────┬─────────────┘│
           │           │ Injects Failures
           └───────────▼────────────────┐
                       │                │
                       ▼                ▼
        ┌───────────────────────────┐  ┌───────────────────┐
        │   Microservices Cluster    │  │ Monitoring & Obs  │
        │ ┌───────────────┐         │  │  Prometheus /     │
        │ │ API Gateway   │◀───────┐ │  │  Grafana / ELK   │
        │ │ Order Service │        │ │  └───────────────────┘
        │ │ Inventory     │        │ │
        │ │ Payment       │        │ │
        │ └───────────────┘        │ │
        └──────────────────────────┘ │
                                     │
                                     ▼
                             ┌─────────────┐
                             │ Database(s) │
                             └─────────────┘
```

## 3. Key Features
1. Centralized Chaos Control (REST API, Scheduler, RBAC)
2. Multi-Level Failure Injection (Infrastructure, Service, Database)
3. Observability (Prometheus, Grafana, Structured Logging)
4. Safety & Guardrails (Feature flags, Canary experiments, Kill switch)
5. Integration (Spring Boot microservices, Kubernetes, CI/CD)

## 4. Spring Boot Skeleton
```java
@SpringBootApplication
@EnableScheduling
public class VikenChaosMonkeyApplication {
    public static void main(String[] args) {
        SpringApplication.run(VikenChaosMonkeyApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {
    private final ChaosService chaosService;
    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }
    @PostMapping("/inject")
    public ResponseEntity<String> injectChaos(@RequestBody ChaosRequest request) {
        chaosService.executeChaos(request);
        return ResponseEntity.ok("Chaos injected successfully!");
    }
}

@Service
public class ChaosService {
    public void executeChaos(ChaosRequest request) {
        System.out.println("Executing chaos: " + request.getType());
    }
}

@Component
public class ChaosScheduler {
    private final ChaosService chaosService;
    public ChaosScheduler(ChaosService chaosService) {
        this.chaosService = chaosService;
    }
    @Scheduled(fixedRateString = "${chaos.interval.ms:3600000}")
    public void runScheduledChaos() {
        chaosService.executeChaos(new ChaosRequest("pod-kill"));
    }
}

public record ChaosRequest(String type, String targetService, Map<String,String> params) {}
```

## 5. Production Readiness
- Deploy in Kubernetes Deployment
- Use ConfigMaps/Secrets for sensitive configs
- Expose Actuator metrics for Prometheus
- Integrate structured logging for ELK or Loki
- Enable RBAC and audit logs

**Team Action Items:**
1. Setup Spring Boot project and repository
2. Implement Controller, Service, Scheduler
3. Integrate Kubernetes API for chaos injection
4. Configure monitoring and dashboards
5. Test in staging before production rollout

