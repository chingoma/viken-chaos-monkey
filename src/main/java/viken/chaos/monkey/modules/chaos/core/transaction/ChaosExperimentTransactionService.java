package viken.chaos.monkey.modules.chaos.core.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentEntity;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentRunEntity;
import viken.chaos.monkey.modules.chaos.core.mapper.ChaosExperimentMapper;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosExperimentRepository;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosExperimentRunRepository;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosSafetyPolicyRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChaosExperimentTransactionService {

    private final ChaosExperimentRepository experimentRepository;
    private final ChaosExperimentRunRepository runRepository;
    private final ChaosSafetyPolicyRepository safetyPolicyRepository;
    private final ChaosExperimentMapper mapper;

    public ChaosExperimentTransactionService(ChaosExperimentRepository experimentRepository,
                                             ChaosExperimentRunRepository runRepository,
                                             ChaosSafetyPolicyRepository safetyPolicyRepository,
                                             ChaosExperimentMapper mapper) {
        this.experimentRepository = experimentRepository;
        this.runRepository = runRepository;
        this.safetyPolicyRepository = safetyPolicyRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<ChaosExperimentEntity> findByUid(String uid) {
        return experimentRepository.findByUid(uid);
    }

    @Transactional(readOnly = true)
    public List<ChaosExperimentEntity> listAll() {
        return experimentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countRunning() {
        return experimentRepository.countByStatusIgnoreCase("RUNNING");
    }

    @Transactional
    public ChaosExperimentEntity createRunning(ChaosExperimentRequest request, String actor, String environment,
                                               String correlationId, int blastRadius) {
        ChaosExperimentEntity entity = new ChaosExperimentEntity();
        entity.setType(request.type());
        entity.setTargetService(request.targetService());
        entity.setNamespace(request.namespace());
        entity.setParamsJson(mapper.writeParams(request));
        entity.setStatus("RUNNING");
        entity.setRequestedBy(actor);
        entity.setExecutedBy(actor);
        entity.setBlastRadiusPercent(blastRadius);
        entity.setEnvironment(environment);
        entity.setStartedAt(Instant.now());
        entity.setCorrelationId(correlationId);
        entity.setUpdatedAt(Instant.now());
        return experimentRepository.save(entity);
    }

    @Transactional
    public ChaosExperimentEntity updateStatus(ChaosExperimentEntity entity, String status, String message,
                                                String adapterUsed) {
        entity.setStatus(status);
        entity.setResultMessage(message);
        entity.setCompletedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        ChaosExperimentEntity saved = experimentRepository.save(entity);

        long runCount = runRepository.countByExperimentId(saved.getId()) + 1;
        ChaosExperimentRunEntity run = new ChaosExperimentRunEntity();
        run.setExperimentId(saved.getId());
        run.setRunNumber((int) runCount);
        run.setStatus(status);
        run.setAdapterUsed(adapterUsed);
        run.setStartedAt(saved.getStartedAt());
        run.setCompletedAt(saved.getCompletedAt());
        if (saved.getStartedAt() != null && saved.getCompletedAt() != null) {
            run.setDurationMs(saved.getCompletedAt().toEpochMilli() - saved.getStartedAt().toEpochMilli());
        }
        run.setErrorDetail("FAILED".equals(status) || "ABORTED".equals(status) ? message : null);
        runRepository.save(run);
        return saved;
    }

    @Transactional
    public void setKillSwitch(String environment, boolean active, String actor) {
        safetyPolicyRepository.findByEnvironmentIgnoreCase(environment).ifPresent(policy -> {
            policy.setKillSwitchActive(active);
            policy.setUpdatedBy(actor);
            policy.setUpdatedAt(Instant.now());
            safetyPolicyRepository.save(policy);
        });
    }

    @Transactional(readOnly = true)
    public boolean isKillSwitchActive(String environment) {
        return safetyPolicyRepository.findByEnvironmentIgnoreCase(environment)
                .map(p -> p.isKillSwitchActive())
                .orElse(true);
    }
}
