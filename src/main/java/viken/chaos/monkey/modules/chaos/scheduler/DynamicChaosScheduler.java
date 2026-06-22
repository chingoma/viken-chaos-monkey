package viken.chaos.monkey.modules.chaos.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosSchedulerConfigEntity;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosSchedulerTransactionService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;
import viken.chaos.monkey.security.ChaosActorResolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DB-driven scheduler polled every minute; executes due cron configs.
 */
@Component
@ConditionalOnProperty(name = "chaos.persistence.mode", havingValue = "db", matchIfMissing = true)
public class DynamicChaosScheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicChaosScheduler.class);
    private static final ZoneId TZ = ZoneId.of("Africa/Dar_es_Salaam");

    private final ChaosSchedulerTransactionService schedulerTransactionService;
    private final ChaosOrchestrationService orchestrationService;
    private final ChaosSafetyService safetyService;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;
    private final ObjectMapper objectMapper;

    public DynamicChaosScheduler(ChaosSchedulerTransactionService schedulerTransactionService,
                                 ChaosOrchestrationService orchestrationService,
                                 ChaosSafetyService safetyService,
                                 Optional<EnvironmentPolicyService> environmentPolicyService,
                                 ObjectMapper objectMapper) {
        this.schedulerTransactionService = schedulerTransactionService;
        this.orchestrationService = orchestrationService;
        this.safetyService = safetyService;
        this.environmentPolicyService = environmentPolicyService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRateString = "${chaos.scheduler.poll-ms:60000}")
    public void pollSchedulers() {
        if (environmentPolicyService.isPresent() && !environmentPolicyService.get().isSchedulerAllowed()) {
            return;
        }
        if (!safetyService.isAllowedToRun() || !safetyService.isWithinTimeWindow()) {
            return;
        }
        for (ChaosSchedulerConfigEntity config : schedulerTransactionService.findEnabled()) {
            if (!isDue(config)) {
                continue;
            }
            runConfig(config);
        }
    }

    private boolean isDue(ChaosSchedulerConfigEntity config) {
        try {
            CronExpression cron = CronExpression.parse(config.getCronExpression());
            ZonedDateTime now = ZonedDateTime.now(TZ);
            if (config.getLastRunAt() == null) {
                return true;
            }
            Instant last = config.getLastRunAt();
            ZonedDateTime next = cron.next(ZonedDateTime.ofInstant(last, TZ));
            return next != null && !next.isAfter(now);
        } catch (Exception e) {
            log.warn("Invalid cron for scheduler {}: {}", config.getName(), e.getMessage());
            return false;
        }
    }

    private void runConfig(ChaosSchedulerConfigEntity config) {
        List<String> targets = readTargets(config.getTargetServices());
        if (targets.isEmpty()) {
            schedulerTransactionService.recordRun(config, "SKIPPED", nextRun(config));
            return;
        }
        String target = targets.get((int) (System.currentTimeMillis() % targets.size()));
        var request = new ChaosExperimentRequest(
                config.getExperimentType(),
                target,
                null,
                Map.of("blastRadius", String.valueOf(config.getDefaultBlastRadius())));
        log.info("DB scheduler {} running type={} target={}", config.getName(), config.getExperimentType(), target);
        var result = orchestrationService.execute(request, "scheduler:" + ChaosActorResolver.currentActor());
        schedulerTransactionService.recordRun(config, result.code(), nextRun(config));
    }

    private Instant nextRun(ChaosSchedulerConfigEntity config) {
        try {
            CronExpression cron = CronExpression.parse(config.getCronExpression());
            ZonedDateTime next = cron.next(ZonedDateTime.now(TZ));
            return next != null ? next.toInstant() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readTargets(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
