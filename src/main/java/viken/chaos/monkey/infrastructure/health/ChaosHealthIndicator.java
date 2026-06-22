package viken.chaos.monkey.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

@Component
public class ChaosHealthIndicator implements HealthIndicator {

    private final ChaosSafetyService safetyService;
    private final ChaosStateStore stateStore;

    public ChaosHealthIndicator(ChaosSafetyService safetyService, ChaosStateStore stateStore) {
        this.safetyService = safetyService;
        this.stateStore = stateStore;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("chaosEnabled", safetyService.isChaosEnabled())
                .withDetail("killSwitchActive", safetyService.isKillSwitchActive())
                .withDetail("runningExperiments", stateStore.countByStatus("RUNNING"))
                .build();
    }
}
