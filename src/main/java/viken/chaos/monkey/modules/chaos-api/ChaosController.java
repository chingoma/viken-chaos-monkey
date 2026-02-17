package viken.chaos.monkey.modules.chaos.api;

import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import viken.chaos.monkey.common.dto.ApiResponse;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ResponseCodes;
import viken.chaos.monkey.modules.chaos.audit.ChaosAuditService;
import viken.chaos.monkey.modules.chaos.core.ChaosOrchestrationService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

import java.util.Map;

/**
 * REST API for chaos experiment control.
 */
@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosOrchestrationService orchestrationService;
    private final ChaosSafetyService safetyService;
    private final ChaosAuditService auditService;

    public ChaosController(ChaosOrchestrationService orchestrationService,
                           ChaosSafetyService safetyService,
                           ChaosAuditService auditService) {
        this.orchestrationService = orchestrationService;
        this.safetyService = safetyService;
        this.auditService = auditService;
    }

    @PostMapping("/experiments")
    public ResponseEntity<ApiResponse<ChaosExperimentResponse>> createExperiment(
            @Valid @RequestBody ChaosExperimentRequest request) {
        String requestId = MDC.get("requestId");

        var result = orchestrationService.execute(request);

        if (result.errorMessage() != null && result.data() == null) {
            return ResponseEntity.ok(ApiResponse.error(result.code(), result.errorMessage(), requestId));
        }
        if (result.code().equals(ResponseCodes.SUCCESS)) {
            return ResponseEntity.ok(ApiResponse.success(result.data(), requestId));
        }
        return ResponseEntity.ok(ApiResponse.of(result.code(), result.data(),
                List.of(result.errorMessage()), requestId));
    }

    @GetMapping("/experiments/{id}")
    public ResponseEntity<ApiResponse<ChaosExperimentResponse>> getExperiment(@PathVariable String id) {
        String requestId = MDC.get("requestId");
        ChaosExperimentResponse experiment = orchestrationService.getExperiment(id);

        if (experiment == null) {
            return ResponseEntity.ok(ApiResponse.error(ResponseCodes.VALIDATION_ERROR,
                    "Experiment not found: " + id, requestId));
        }

        return ResponseEntity.ok(ApiResponse.success(experiment, requestId));
    }

    @GetMapping("/experiments")
    public ResponseEntity<ApiResponse<List<ChaosExperimentResponse>>> listExperiments() {
        String requestId = MDC.get("requestId");
        return ResponseEntity.ok(ApiResponse.success(orchestrationService.listExperiments(), requestId));
    }

    @DeleteMapping("/experiments/{id}")
    public ResponseEntity<ApiResponse<ChaosExperimentResponse>> abortExperiment(@PathVariable String id) {
        String requestId = MDC.get("requestId");
        var result = orchestrationService.abortExperiment(id);
        if (!result.aborted()) {
            return ResponseEntity.ok(ApiResponse.error(ResponseCodes.VALIDATION_ERROR,
                    result.errorMessage(), requestId));
        }
        return ResponseEntity.ok(ApiResponse.success(result.data(), requestId));
    }

    @PostMapping("/emergency/disable")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> activateKillSwitch() {
        String requestId = MDC.get("requestId");
        safetyService.activateKillSwitch();
        auditService.auditKillSwitchActivated("api");
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("killSwitchActive", true), requestId));
    }

    @PostMapping("/emergency/enable")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deactivateKillSwitch() {
        String requestId = MDC.get("requestId");
        safetyService.deactivateKillSwitch();
        auditService.auditKillSwitchDeactivated("api");
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("killSwitchActive", false), requestId));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        String requestId = MDC.get("requestId");
        Map<String, Object> status = Map.of(
                "chaosEnabled", safetyService.isChaosEnabled(),
                "killSwitchActive", safetyService.isKillSwitchActive(),
                "allowedToRun", safetyService.isAllowedToRun()
        );
        return ResponseEntity.ok(ApiResponse.success(status, requestId));
    }
}
