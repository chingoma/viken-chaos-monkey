package viken.chaos.monkey.modules.chaos.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.core.ChaosOrchestrationService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

import java.util.List;
import java.util.Map;

/**
 * Scheduled chaos experiments. Runs at configured interval when enabled.
 */
@Component
@ConditionalOnProperty(name = "chaos.scheduler.enabled", havingValue = "true")
public class ChaosScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChaosScheduler.class);

    private final ChaosOrchestrationService orchestrationService;
    private final ChaosSafetyService safetyService;
    private final ChaosSafetyProperties properties;

    public ChaosScheduler(ChaosOrchestrationService orchestrationService,
                         ChaosSafetyService safetyService,
                         ChaosSafetyProperties properties) {
        this.orchestrationService = orchestrationService;
        this.safetyService = safetyService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${chaos.scheduler.interval-ms:3600000}")
    public void runScheduledChaos() {
        if (!safetyService.isAllowedToRun()) {
            log.debug("Scheduled chaos skipped: chaos disabled or kill switch active");
            return;
        }

        if (!safetyService.isWithinTimeWindow()) {
            log.debug("Scheduled chaos skipped: outside time window");
            return;
        }

        List<String> targets = properties.getScheduledTargets();
        if (targets == null || targets.isEmpty()) {
            log.debug("Scheduled chaos skipped: no targets configured");
            return;
        }

        String type = properties.getScheduledExperimentType();
        if (type == null || type.isBlank()) {
            type = "pod-kill";
        }

        String target = targets.get((int) (System.currentTimeMillis() % targets.size()));
        var request = new ChaosExperimentRequest(type, target, null, Map.of("blastRadius", "5"));

        log.info("Running scheduled chaos: type={}, target={}", type, target);
        var result = orchestrationService.execute(request);
        log.info("Scheduled chaos result: code={}, message={}", result.code(),
                result.data() != null ? result.data().message() : result.errorMessage());
    }
}
