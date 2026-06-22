package viken.chaos.monkey.modules.chaos.core.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.dse.trading.core.api.ApiPayloadCode;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ChaosResponseCodes;
import viken.chaos.monkey.common.util.Uuid7Generator;
import viken.chaos.monkey.modules.chaos.adapters.ChaosAdapter;
import viken.chaos.monkey.modules.chaos.audit.ChaosAuditService;
import viken.chaos.monkey.modules.chaos.audit.ChaosMetricsService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosApprovalService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService.AbortResult;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService.ChaosExecutionResult;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class ChaosOrchestrationServiceImpl implements ChaosOrchestrationService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ABORTED = "ABORTED";
    private static final String STATUS_PENDING = "PENDING_APPROVAL";

    private final ChaosSafetyService safetyService;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;
    private final Optional<ChaosApprovalService> approvalService;
    private final List<ChaosAdapter> adapters;
    private final ChaosMetricsService metricsService;
    private final ChaosAuditService auditService;
    private final ChaosStateStore stateStore;
    private final Executor executionExecutor;
    private final String environment;
    private final Map<String, CompletableFuture<Void>> runningExperiments = new ConcurrentHashMap<>();

    public ChaosOrchestrationServiceImpl(ChaosSafetyService safetyService,
                                         Optional<EnvironmentPolicyService> environmentPolicyService,
                                         Optional<ChaosApprovalService> approvalService,
                                         List<ChaosAdapter> adapters,
                                         ChaosMetricsService metricsService,
                                         ChaosAuditService auditService,
                                         ChaosStateStore stateStore,
                                         @Qualifier("chaosExecutionExecutor") Executor executionExecutor,
                                         @Value("${chaos.environment:LOCAL}") String environment) {
        this.safetyService = safetyService;
        this.environmentPolicyService = environmentPolicyService;
        this.approvalService = approvalService;
        this.adapters = adapters != null ? adapters : List.of();
        this.metricsService = metricsService;
        this.auditService = auditService;
        this.stateStore = stateStore;
        this.executionExecutor = executionExecutor;
        this.environment = environment;
    }

    @Override
    public ChaosExecutionResult execute(ChaosExperimentRequest request, String actor) {
        ChaosAdapter adapter = findAdapter(request.type());
        var validation = validate(request, adapter);
        if (validation != null) {
            return ChaosExecutionResult.validationFailed(validation.code, validation.message);
        }

        if (requiresApproval()) {
            return approvalService
                    .map(service -> service.submitForApproval(request, actor, environment))
                    .orElse(ChaosExecutionResult.validationFailed(
                            ChaosResponseCodes.APPROVAL_REQUIRED.getCode(),
                            "Approval required but approval service unavailable"));
        }

        return dispatchExperiment(request, actor, adapter);
    }

    private ChaosExecutionResult dispatchExperiment(ChaosExperimentRequest request, String actor,
                                                      ChaosAdapter adapter) {
        String experimentId = Uuid7Generator.generateString();
        Instant startedAt = Instant.now();

        ChaosExperimentResponse running = ChaosExperimentResponse.of(
                experimentId, request.type(), request.targetService(),
                STATUS_RUNNING, "Experiment accepted for execution", startedAt, null);
        stateStore.upsertExperiment(running);
        auditService.auditExperimentTriggered(request, experimentId, actor);

        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () -> runExperiment(adapter, request, experimentId, startedAt),
                            executionExecutor)
                    .whenComplete((unused, throwable) -> runningExperiments.remove(experimentId));
            runningExperiments.put(experimentId, future);
            return ChaosExecutionResult.success(running);
        } catch (RejectedExecutionException e) {
            return markFailed(experimentId, request, startedAt, "Execution capacity exhausted");
        }
    }

    private void runExperiment(ChaosAdapter adapter, ChaosExperimentRequest request,
                               String experimentId, Instant startedAt) {
        try {
            if (Thread.currentThread().isInterrupted() || isAlreadyAborted(experimentId)) {
                return;
            }
            ChaosAdapter.ChaosExperimentResult result = adapter.execute(request);
            Instant completedAt = Instant.now();
            String status = normalizeStatus(result.status());
            if (isAlreadyAborted(experimentId)) {
                return;
            }
            ChaosExperimentResponse response = ChaosExperimentResponse.of(
                    experimentId, request.type(), request.targetService(),
                    status, result.message(), startedAt, completedAt);
            stateStore.upsertExperiment(response);
            long durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
            metricsService.recordExperimentExecuted(request.type(), request.targetService(), status, durationMs);
            if (STATUS_FAILED.equals(status)) {
                auditService.auditExperimentFailed(experimentId, request.type(), request.targetService(),
                        result.message());
                return;
            }
            auditService.auditExperimentCompleted(response);
        } catch (Exception e) {
            if (!isAlreadyAborted(experimentId)) {
                markFailedInternal(experimentId, request, startedAt, e.getMessage());
            }
        }
    }

    private ChaosValidationResult validate(ChaosExperimentRequest request, ChaosAdapter adapter) {
        if (!safetyService.isChaosEnabled()) {
            return new ChaosValidationResult(ChaosResponseCodes.CHAOS_DISABLED.getCode(), "Chaos is disabled");
        }
        if (safetyService.isKillSwitchActive()) {
            return new ChaosValidationResult(ChaosResponseCodes.KILL_SWITCH_ACTIVE.getCode(), "Kill switch is active");
        }
        if (safetyService.isTargetExcluded(request.targetService())) {
            return new ChaosValidationResult(ChaosResponseCodes.TARGET_EXCLUDED.getCode(),
                    "Target service is excluded: " + request.targetService());
        }
        if (!safetyService.isExperimentTypeEnabled(request.type())) {
            return new ChaosValidationResult(ChaosResponseCodes.EXPERIMENT_TYPE_DISABLED.getCode(),
                    "Experiment type disabled: " + request.type());
        }
        int blastRadius = request.getParamAsInt("blastRadius", 1);
        if (!safetyService.isBlastRadiusWithinLimit(blastRadius)) {
            return new ChaosValidationResult(ChaosResponseCodes.BLAST_RADIUS_EXCEEDED.getCode(),
                    "Blast radius must be between 1 and " + safetyService.getMaxBlastRadiusPercent());
        }
        if (!safetyService.isWithinTimeWindow()) {
            return new ChaosValidationResult(ChaosResponseCodes.TIME_WINDOW_VIOLATION.getCode(),
                    "Outside allowed chaos time window");
        }
        if (environmentPolicyService.isPresent()) {
            EnvironmentPolicyService policy = environmentPolicyService.get();
            if (policy.isDailyLimitExceeded()) {
                return new ChaosValidationResult(ChaosResponseCodes.TIME_WINDOW_VIOLATION.getCode(),
                        "Daily experiment limit exceeded for environment " + environment);
            }
            String ns = request.namespace() != null ? request.namespace() : "default";
            if (!policy.isNamespaceAllowed(ns)) {
                return new ChaosValidationResult(ChaosResponseCodes.TARGET_EXCLUDED.getCode(),
                        "Namespace not allowed: " + ns);
            }
        }
        if (adapter == null) {
            return new ChaosValidationResult(ChaosResponseCodes.ADAPTER_ERROR.getCode(),
                    "No adapter for experiment type: " + request.type());
        }
        return null;
    }

    private boolean requiresApproval() {
        return environmentPolicyService.map(EnvironmentPolicyService::isApprovalRequired).orElse(false);
    }

    @Override
    public ChaosExperimentResponse getExperiment(String id) {
        return stateStore.getExperiment(id);
    }

    @Override
    public List<ChaosExperimentResponse> listExperiments() {
        return stateStore.listExperiments();
    }

    @Override
    public AbortResult abortExperiment(String id, String actor) {
        ChaosExperimentResponse existing = stateStore.getExperiment(id);
        if (existing == null) {
            return AbortResult.notFound("Experiment not found: " + id);
        }
        if (!STATUS_RUNNING.equalsIgnoreCase(existing.status())) {
            return AbortResult.notRunning("Experiment is not running: " + id);
        }
        CompletableFuture<Void> future = runningExperiments.remove(id);
        if (future != null) {
            future.cancel(true);
        }
        Instant completedAt = Instant.now();
        ChaosExperimentResponse aborted = ChaosExperimentResponse.of(
                existing.experimentId(), existing.type(), existing.targetService(),
                STATUS_ABORTED, "Experiment aborted by operator", existing.startedAt(), completedAt);
        stateStore.upsertExperiment(aborted);
        long durationMs = existing.startedAt() != null
                ? Math.max(0, completedAt.toEpochMilli() - existing.startedAt().toEpochMilli()) : 0;
        metricsService.recordExperimentExecuted(existing.type(), existing.targetService(), STATUS_ABORTED, durationMs);
        auditService.auditExperimentFailed(existing.experimentId(), existing.type(),
                existing.targetService(), "Aborted by " + actor);
        return AbortResult.aborted(aborted);
    }

    private ChaosExecutionResult markFailed(String experimentId, ChaosExperimentRequest request,
                                            Instant startedAt, String message) {
        ChaosExperimentResponse failed = markFailedInternal(experimentId, request, startedAt, message);
        return ChaosExecutionResult.error(failed);
    }

    private ChaosExperimentResponse markFailedInternal(String experimentId, ChaosExperimentRequest request,
                                                       Instant startedAt, String message) {
        Instant completedAt = Instant.now();
        ChaosExperimentResponse failed = ChaosExperimentResponse.of(
                experimentId, request.type(), request.targetService(),
                STATUS_FAILED, message, startedAt, completedAt);
        stateStore.upsertExperiment(failed);
        long durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
        metricsService.recordExperimentExecuted(request.type(), request.targetService(), STATUS_FAILED, durationMs);
        auditService.auditExperimentFailed(experimentId, request.type(), request.targetService(), message);
        return failed;
    }

    private ChaosAdapter findAdapter(String type) {
        return adapters.stream().filter(a -> a.supports(type)).findFirst().orElse(null);
    }

    private boolean isAlreadyAborted(String experimentId) {
        ChaosExperimentResponse current = stateStore.getExperiment(experimentId);
        return current != null && STATUS_ABORTED.equalsIgnoreCase(current.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_FAILED;
        }
        return status.toUpperCase(Locale.ROOT);
    }

    private record ChaosValidationResult(String code, String message) {}
}
