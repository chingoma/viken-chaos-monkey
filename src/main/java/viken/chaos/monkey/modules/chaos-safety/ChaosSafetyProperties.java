package viken.chaos.monkey.modules.chaos.safety;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "chaos")
public class ChaosSafetyProperties {

    private boolean enabled = false;
    private boolean killSwitchActive = false;
    private int blastRadiusMaxPercent = 10;
    private List<String> excludedServices = List.of("payment-service", "auth-service");
    private List<String> enabledTypes = new ArrayList<>();
    private LocalTime chaosWindowStart;
    private LocalTime chaosWindowEnd;
    private List<String> scheduledTargets = new ArrayList<>();
    private String scheduledExperimentType = "pod-kill";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isKillSwitchActive() {
        return killSwitchActive;
    }

    public void setKillSwitchActive(boolean killSwitchActive) {
        this.killSwitchActive = killSwitchActive;
    }

    public int getBlastRadiusMaxPercent() {
        return blastRadiusMaxPercent;
    }

    public void setBlastRadiusMaxPercent(int blastRadiusMaxPercent) {
        this.blastRadiusMaxPercent = blastRadiusMaxPercent;
    }

    public List<String> getExcludedServices() {
        return excludedServices;
    }

    public void setExcludedServices(List<String> excludedServices) {
        this.excludedServices = excludedServices != null ? excludedServices : List.of();
    }

    public List<String> getEnabledTypes() {
        return enabledTypes;
    }

    public void setEnabledTypes(List<String> enabledTypes) {
        this.enabledTypes = enabledTypes != null ? enabledTypes : new ArrayList<>();
    }

    public LocalTime getChaosWindowStart() {
        return chaosWindowStart;
    }

    public void setChaosWindowStart(LocalTime chaosWindowStart) {
        this.chaosWindowStart = chaosWindowStart;
    }

    public LocalTime getChaosWindowEnd() {
        return chaosWindowEnd;
    }

    public void setChaosWindowEnd(LocalTime chaosWindowEnd) {
        this.chaosWindowEnd = chaosWindowEnd;
    }

    public List<String> getScheduledTargets() {
        return scheduledTargets;
    }

    public void setScheduledTargets(List<String> scheduledTargets) {
        this.scheduledTargets = scheduledTargets != null ? scheduledTargets : new ArrayList<>();
    }

    public String getScheduledExperimentType() {
        return scheduledExperimentType;
    }

    public void setScheduledExperimentType(String scheduledExperimentType) {
        this.scheduledExperimentType = scheduledExperimentType;
    }
}
