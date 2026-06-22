package viken.chaos.monkey.modules.chaos.safety;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosEnvironmentPolicyEntity;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosSafetyPolicyEntity;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosEnvironmentPolicyRepository;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosSafetyPolicyRepository;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * Environment policy enforcement backed by database when persistence.mode=db.
 */
@Service
@ConditionalOnProperty(name = "chaos.persistence.mode", havingValue = "db", matchIfMissing = true)
public class EnvironmentPolicyService {

    private final ChaosEnvironmentPolicyRepository environmentRepository;
    private final ChaosSafetyPolicyRepository safetyPolicyRepository;
    private final viken.chaos.monkey.modules.chaos.core.store.ChaosExperimentCounter experimentCounter;
    private final ObjectMapper objectMapper;
    private final String environment;

    public EnvironmentPolicyService(ChaosEnvironmentPolicyRepository environmentRepository,
                                    ChaosSafetyPolicyRepository safetyPolicyRepository,
                                    viken.chaos.monkey.modules.chaos.core.store.ChaosExperimentCounter experimentCounter,
                                    ObjectMapper objectMapper,
                                    @Value("${chaos.environment:LOCAL}") String environment) {
        this.environmentRepository = environmentRepository;
        this.environment = environment;
        this.safetyPolicyRepository = safetyPolicyRepository;
        this.experimentCounter = experimentCounter;
        this.objectMapper = objectMapper;
    }

    public ChaosEnvironmentPolicyEntity getEnvironmentPolicy() {
        return environmentRepository.findByEnvironmentIgnoreCase(environment)
                .orElseThrow(() -> new IllegalStateException("No environment policy for: " + environment));
    }

    public ChaosSafetyPolicyEntity getSafetyPolicy() {
        return safetyPolicyRepository.findByEnvironmentIgnoreCase(environment)
                .orElseThrow(() -> new IllegalStateException("No safety policy for: " + environment));
    }

    public boolean isRealAdapterAllowed() {
        return getEnvironmentPolicy().isAllowRealAdapters();
    }

    public boolean isSchedulerAllowed() {
        return getEnvironmentPolicy().isAllowScheduler();
    }

    public boolean isApprovalRequired() {
        return getEnvironmentPolicy().isRequireApproval();
    }

    public boolean isNamespaceAllowed(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        List<String> allowed = readJsonList(getEnvironmentPolicy().getAllowedNamespaces());
        return allowed.isEmpty() || allowed.stream().anyMatch(n -> n.equalsIgnoreCase(namespace));
    }

    public boolean isDailyLimitExceeded() {
        ChaosEnvironmentPolicyEntity policy = getEnvironmentPolicy();
        Instant since = Instant.now().atZone(ZoneId.of("Africa/Dar_es_Salaam")).toLocalDate()
                .atStartOfDay(ZoneId.of("Africa/Dar_es_Salaam")).toInstant();
        long count = experimentCounter.countSince(environment, since);
        return count >= policy.getMaxDailyExperiments();
    }

    public boolean isChaosEnabledFromDb() {
        return getSafetyPolicy().isChaosEnabled();
    }

    public boolean isKillSwitchActiveFromDb() {
        return getSafetyPolicy().isKillSwitchActive();
    }

    public int getMaxBlastRadiusFromDb() {
        return getSafetyPolicy().getBlastRadiusMaxPercent();
    }

    public List<String> getExcludedServicesFromDb() {
        return readJsonList(getSafetyPolicy().getExcludedServices());
    }

    public List<String> getEnabledTypesFromDb() {
        return readJsonList(getSafetyPolicy().getEnabledTypes());
    }

    public boolean isWithinTimeWindowFromDb() {
        ChaosSafetyPolicyEntity policy = getSafetyPolicy();
        LocalTime start = policy.getWindowStart();
        LocalTime end = policy.getWindowEnd();
        if (start == null || end == null) {
            return true;
        }
        LocalTime now = LocalTime.now(ZoneId.of("Africa/Dar_es_Salaam"));
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isAfter(end) || !now.isBefore(start);
    }

    private List<String> readJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
