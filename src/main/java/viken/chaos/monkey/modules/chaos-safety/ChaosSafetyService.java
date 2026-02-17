package viken.chaos.monkey.modules.chaos.safety;

import org.springframework.stereotype.Service;
import viken.chaos.monkey.modules.chaos.core.ChaosStateRepository;

import java.time.LocalTime;

/**
 * Safety engine: kill switch, blast radius, exclusions, time windows.
 */
@Service
public class ChaosSafetyService {

    private final ChaosSafetyProperties properties;
    private final ChaosStateRepository stateRepository;

    public ChaosSafetyService(ChaosSafetyProperties properties, ChaosStateRepository stateRepository) {
        this.properties = properties;
        this.stateRepository = stateRepository;
    }

    public boolean isChaosEnabled() {
        return properties.isEnabled();
    }

    public boolean isKillSwitchActive() {
        return stateRepository.isKillSwitchActive();
    }

    public boolean isAllowedToRun() {
        return isChaosEnabled() && !isKillSwitchActive();
    }

    public boolean isTargetExcluded(String targetService) {
        return properties.getExcludedServices().stream()
                .anyMatch(excluded -> excluded.equalsIgnoreCase(targetService));
    }

    public boolean isExperimentTypeEnabled(String type) {
        return properties.getEnabledTypes().isEmpty()
                || properties.getEnabledTypes().stream().anyMatch(t -> t.equalsIgnoreCase(type));
    }

    public boolean isWithinTimeWindow() {
        if (properties.getChaosWindowStart() == null || properties.getChaosWindowEnd() == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        LocalTime start = properties.getChaosWindowStart();
        LocalTime end = properties.getChaosWindowEnd();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isAfter(end) || !now.isBefore(start);
    }

    public int getMaxBlastRadiusPercent() {
        return properties.getBlastRadiusMaxPercent();
    }

    public boolean isBlastRadiusWithinLimit(int blastRadiusPercent) {
        return blastRadiusPercent > 0 && blastRadiusPercent <= properties.getBlastRadiusMaxPercent();
    }

    public void activateKillSwitch() {
        stateRepository.setKillSwitchActive(true);
    }

    public void deactivateKillSwitch() {
        stateRepository.setKillSwitchActive(false);
    }
}
