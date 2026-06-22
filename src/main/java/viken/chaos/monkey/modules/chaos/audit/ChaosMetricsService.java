package viken.chaos.monkey.modules.chaos.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

@Service
public class ChaosMetricsService {

    private static final String PREFIX = "chaos_";
    private final MeterRegistry registry;

    public ChaosMetricsService(MeterRegistry registry, ChaosSafetyService safetyService,
                               ChaosStateStore stateStore) {
        this.registry = registry;

        Gauge.builder(PREFIX + "kill_switch_active", safetyService, s -> s.isKillSwitchActive() ? 1 : 0)
                .description("1 if kill switch is active, 0 otherwise")
                .register(registry);

        Gauge.builder(PREFIX + "experiments_active", stateStore, s -> s.countByStatus("RUNNING"))
                .description("Number of currently running experiments")
                .register(registry);
    }

    public void recordExperimentExecuted(String type, String targetService, String status, long durationMs) {
        Counter.builder(PREFIX + "experiments_total")
                .tag("type", type)
                .tag("target", targetService)
                .tag("status", status)
                .register(registry)
                .increment();

        if ("FAILED".equals(status)) {
            Counter.builder(PREFIX + "experiments_failed_total")
                    .tag("type", type)
                    .tag("target", targetService)
                    .register(registry)
                    .increment();
        }

        Timer.builder(PREFIX + "experiment_duration_seconds")
                .tag("type", type)
                .description("Chaos experiment duration")
                .register(registry)
                .record(java.time.Duration.ofMillis(durationMs));
    }
}
