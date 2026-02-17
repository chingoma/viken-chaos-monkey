package viken.chaos.monkey.modules.chaos.core;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ResponseCodes;
import viken.chaos.monkey.common.util.Uuid7Generator;
import viken.chaos.monkey.modules.chaos.adapters.ChaosAdapter;
import viken.chaos.monkey.modules.chaos.audit.ChaosAuditService;
import viken.chaos.monkey.modules.chaos.audit.ChaosMetricsService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * Orchestrates chaos experiments: validation, safety checks, adapter dispatch.
 */
@Service
public class ChaosOrchestrationService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ABORTED = "ABORTED";

    private final ChaosSafetyService safetyService;
    private final List<ChaosAdapter> adapters;
    private final ChaosMetricsService metricsService;
    private final ChaosAuditService auditService;
    private final ChaosStateRepository stateRepository;
    private final Executor executionExecutor;
    private final Map<String, CompletableFuture<Void>> runningExperiments = new ConcurrentHashMap<>();

    public ChaosOrchestrationService(ChaosSafetyService safetyService, List<ChaosAdapter> adapters,
                                     ChaosMetricsService metricsService, ChaosAuditService auditService,
                                     ChaosStateRepository stateRepository,
                                     @Qualifier("chaosExecutionExecutor") Executor executionExecutor) {
        this.safetyService = safetyService;
        this.adapters = adapters != null ? adapters : List.of();
        this.metricsService = metricsService;
        this.auditService = auditService;
        this.stateRepository = stateRepository;
        this.executionExecutor = executionExecutor;
    }

    public ChaosExecutionResult execute(ChaosExperimentRequest request) {
        ChaosAdapter adapter = findAdapter(request.type());
        var validation = validate(request, adapter);
        if (validation != null) {
            return ChaosExecutionResult.validationFailed(validation.code, validation.message);
        }

        String experimentId = Uuid7Generator.generateString();
        Instant startedAt = Instant.now();

        ChaosExperimentResponse running = ChaosExperimentResponse.of(
                experimentId,
                request.type(),
                request.targetService(),
                STATUS_RUNNING,
                "Experiment accepted for execution",
                startedAt,
                null
        );
        stateRepository.upsertExperiment(running);
        auditService.auditExperimentTriggered(request, experimentId, "api");

        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () -> runExperiment(adapter, request, experimentId, startedAt),
                            executionExecutor
                    )
                    .whenComplete((unused, throwable) -> runningExperiments.remove(experimentId));
            runningExperiments.put(experimentId, future);
            return ChaosExecutionResult.success(running);
        } catch (RejectedExecutionException e) {
            ChaosExperimentResponse failed = ChaosExperimentResponse.of(
                    experimentId,
                    request.type(),
                    request.targetService(),
                    STATUS_FAILED,
                    "Execution capacity exhausted",
                    startedAt,
                    Instant.now()
            );
            stateRepository.upsertExperiment(failed);
            metricsService.recordExperimentExecuted(request.type(), request.targetService(), STATUS_FAILED, 0);
            auditService.auditExperimentFailed(experimentId, request.type(), request.targetService(),
                    "Execution capacity exhausted");
            return ChaosExecutionResult.error(failed);
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
                    experimentId,
                    request.type(),
                    request.targetService(),
                    status,
                    result.message(),
                    startedAt,
                    completedAt
            );
            stateRepository.upsertExperiment(response);

            long durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
            metricsService.recordExperimentExecuted(request.type(), request.targetService(), status, durationMs);
            if (STATUS_FAILED.equals(status)) {
                auditService.auditExperimentFailed(experimentId, request.type(), request.targetService(), result.message());
                return;
            }
            auditService.auditExperimentCompleted(response);
        } catch (Exception e) {
            if (isAlreadyAborted(experimentId)) {
                return;
            }
            Instant completedAt = Instant.now();
            ChaosExperimentResponse failed = ChaosExperimentResponse.of(
                    experimentId,
                    request.type(),
                    request.targetService(),
                    STATUS_FAILED,
                    e.getMessage(),
                    startedAt,
                    completedAt
            );
            stateRepository.upsertExperiment(failed);
            long durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
            metricsService.recordExperimentExecuted(request.type(), request.targetService(), STATUS_FAILED, durationMs);
            auditService.auditExperimentFailed(experimentId, request.type(), request.targetService(), e.getMessage());
        }
    }

    private ChaosValidationResult validate(ChaosExperimentRequest request, ChaosAdapter adapter) {
        if (!safetyService.isChaosEnabled()) {
            return new ChaosValidationResult(ResponseCodes.CHAOS_DISABLED, "Chaos is disabled");
        }
        if (safetyService.isKillSwitchActive()) {
            return new ChaosValidationResult(ResponseCodes.KILL_SWITCH_ACTIVE, "Kill switch is active");
        }
        if (safetyService.isTargetExcluded(request.targetService())) {
            return new ChaosValidationResult(ResponseCodes.TARGET_EXCLUDED,
                    "Target service is excluded: " + request.targetService());
        }
        if (!safetyService.isExperimentTypeEnabled(request.type())) {
            return new ChaosValidationResult(ResponseCodes.EXPERIMENT_TYPE_DISABLED,
                    "Experiment type disabled: " + request.type());
        }
        int blastRadius = request.getParamAsInt("blastRadius", 1);
        if (!safetyService.isBlastRadiusWithinLimit(blastRadius)) {
            return new ChaosValidationResult(ResponseCodes.BLAST_RADIUS_EXCEEDED,
                    "Blast radius must be between 1 and " + safetyService.getMaxBlastRadiusPercent());
        }
        if (!safetyService.isWithinTimeWindow()) {
            return new ChaosValidationResult(ResponseCodes.TIME_WINDOW_VIOLATION,
                    "Outside allowed chaos time window");
        }
        if (adapter == null) {
            return new ChaosValidationResult(ResponseCodes.ADAPTER_ERROR,
                    "No adapter for experiment type: " + request.type());
        }
        return null;
    }

    private ChaosAdapter findAdapter(String type) {
        return adapters.stream()
                .filter(a -> a.supports(type))
                .findFirst()
                .orElse(null);
    }

    public ChaosExperimentResponse getExperiment(String id) {
        return stateRepository.getExperiment(id);
    }

    public List<ChaosExperimentResponse> listExperiments() {
        return stateRepository.listExperiments();
    }

    public AbortResult abortExperiment(String id) {
        ChaosExperimentResponse existing = stateRepository.getExperiment(id);
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
                existing.experimentId(),
                existing.type(),
                existing.targetService(),
                STATUS_ABORTED,
                "Experiment aborted by operator",
                existing.startedAt(),
                completedAt
        );
        stateRepository.upsertExperiment(aborted);

        long durationMs = existing.startedAt() != null
                ? Math.max(0, completedAt.toEpochMilli() - existing.startedAt().toEpochMilli())
                : 0;
        metricsService.recordExperimentExecuted(existing.type(), existing.targetService(), STATUS_ABORTED, durationMs);
        auditService.auditExperimentFailed(existing.experimentId(), existing.type(),
                existing.targetService(), "Aborted by operator");
        return AbortResult.aborted(aborted);
    }

    private boolean isAlreadyAborted(String experimentId) {
        ChaosExperimentResponse current = stateRepository.getExperiment(experimentId);
        return current != null && STATUS_ABORTED.equalsIgnoreCase(current.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_FAILED;
        }
        return status.toUpperCase(Locale.ROOT);
    }

    public record ChaosExecutionResult(String code, ChaosExperimentResponse data, String errorMessage) {
        static ChaosExecutionResult success(ChaosExperimentResponse data) {
            return new ChaosExecutionResult(ResponseCodes.SUCCESS, data, null);
        }

        static ChaosExecutionResult validationFailed(String code, String message) {
            return new ChaosExecutionResult(code, null, message);
        }

        static ChaosExecutionResult error(ChaosExperimentResponse data) {
            return new ChaosExecutionResult(ResponseCodes.ADAPTER_ERROR, data, data.message());
        }
    }

    public record AbortResult(boolean aborted, ChaosExperimentResponse data, String errorMessage) {
        static AbortResult aborted(ChaosExperimentResponse data) {
            return new AbortResult(true, data, null);
        }

        static AbortResult notFound(String message) {
            return new AbortResult(false, null, message);
        }

        static AbortResult notRunning(String message) {
            return new AbortResult(false, null, message);
        }
    }

    private record ChaosValidationResult(String code, String message) {}
}
