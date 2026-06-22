package viken.chaos.monkey.modules.chaos.safety;

import org.springframework.stereotype.Service;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Safety engine: kill switch, blast radius, exclusions, time windows.
 */
@Service
public class ChaosSafetyService {

    private static final ZoneId TZ = ZoneId.of("Africa/Dar_es_Salaam");

    private final ChaosSafetyProperties properties;
    private final ChaosStateStore stateStore;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;

    public ChaosSafetyService(ChaosSafetyProperties properties,
                              ChaosStateStore stateStore,
                              Optional<EnvironmentPolicyService> environmentPolicyService) {
        this.properties = properties;
        this.stateStore = stateStore;
        this.environmentPolicyService = environmentPolicyService;
    }

    public boolean isChaosEnabled() {
        return environmentPolicyService.map(EnvironmentPolicyService::isChaosEnabledFromDb)
                .orElseGet(properties::isEnabled);
    }

    public boolean isKillSwitchActive() {
        return environmentPolicyService.map(EnvironmentPolicyService::isKillSwitchActiveFromDb)
                .orElseGet(stateStore::isKillSwitchActive);
    }

    public boolean isAllowedToRun() {
        return isChaosEnabled() && !isKillSwitchActive();
    }

    public boolean isTargetExcluded(String targetService) {
        List<String> excluded = environmentPolicyService
                .map(EnvironmentPolicyService::getExcludedServicesFromDb)
                .orElse(properties.getExcludedServices());
        return excluded.stream().anyMatch(excludedService -> excludedService.equalsIgnoreCase(targetService));
    }

    public boolean isExperimentTypeEnabled(String type) {
        List<String> enabledTypes = environmentPolicyService
                .map(EnvironmentPolicyService::getEnabledTypesFromDb)
                .orElse(properties.getEnabledTypes());
        return enabledTypes.isEmpty()
                || enabledTypes.stream().anyMatch(t -> t.equalsIgnoreCase(type));
    }

    public boolean isWithinTimeWindow() {
        if (environmentPolicyService.isPresent()) {
            return environmentPolicyService.get().isWithinTimeWindowFromDb();
        }
        if (properties.getChaosWindowStart() == null || properties.getChaosWindowEnd() == null) {
            return true;
        }
        LocalTime now = LocalTime.now(TZ);
        LocalTime start = properties.getChaosWindowStart();
        LocalTime end = properties.getChaosWindowEnd();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isAfter(end) || !now.isBefore(start);
    }

    public int getMaxBlastRadiusPercent() {
        return environmentPolicyService.map(EnvironmentPolicyService::getMaxBlastRadiusFromDb)
                .orElseGet(properties::getBlastRadiusMaxPercent);
    }

    public boolean isBlastRadiusWithinLimit(int blastRadiusPercent) {
        return blastRadiusPercent > 0 && blastRadiusPercent <= getMaxBlastRadiusPercent();
    }

    public void activateKillSwitch() {
        stateStore.setKillSwitchActive(true);
    }

    public void deactivateKillSwitch() {
        stateStore.setKillSwitchActive(false);
    }
}
