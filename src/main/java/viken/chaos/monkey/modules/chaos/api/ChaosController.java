package viken.chaos.monkey.modules.chaos.api;

import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.dse.trading.core.api.ApiPayloadCode;
import tz.dse.trading.core.api.GenericRestResponse;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ChaosResponseCodes;
import viken.chaos.monkey.modules.chaos.audit.ChaosAuditService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosApprovalService;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;
import viken.chaos.monkey.security.ChaosActorResolver;
import viken.chaos.monkey.security.ChaosPermissionCodes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosOrchestrationService orchestrationService;
    private final ChaosSafetyService safetyService;
    private final ChaosAuditService auditService;
    private final Optional<ChaosApprovalService> approvalService;

    public ChaosController(ChaosOrchestrationService orchestrationService,
                           ChaosSafetyService safetyService,
                           ChaosAuditService auditService,
                           Optional<ChaosApprovalService> approvalService) {
        this.orchestrationService = orchestrationService;
        this.safetyService = safetyService;
        this.auditService = auditService;
        this.approvalService = approvalService;
    }

    @PostMapping("/experiments")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.EXPERIMENT_CREATE + "') "
            + "or hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<GenericRestResponse<ChaosExperimentResponse>> createExperiment(
            @Valid @RequestBody ChaosExperimentRequest request) {
        String requestId = MDC.get("requestId");
        String actor = ChaosActorResolver.currentActor();
        var result = orchestrationService.execute(request, actor);
        if (result.errorMessage() != null && result.data() == null) {
            return ResponseEntity.ok(GenericRestResponse.of(
                    result.code(), null, java.time.Instant.now().toString(),
                    List.of(result.errorMessage()), requestId));
        }
        if (ApiPayloadCode.SUCCESS.getCode().equals(result.code())) {
            return ResponseEntity.ok(GenericRestResponse.success("Experiment accepted", result.data(), requestId));
        }
        return ResponseEntity.ok(GenericRestResponse.of(
                result.code(), result.data(), java.time.Instant.now().toString(),
                List.of(result.errorMessage()), requestId));
    }

    @GetMapping("/experiments/{id}")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.EXPERIMENT_READ + "') "
            + "or hasAnyRole('VIEWER','AUDITOR','OPERATOR','ADMIN')")
    public ResponseEntity<GenericRestResponse<ChaosExperimentResponse>> getExperiment(@PathVariable String id) {
        String requestId = MDC.get("requestId");
        ChaosExperimentResponse experiment = orchestrationService.getExperiment(id);
        if (experiment == null) {
            return ResponseEntity.ok(GenericRestResponse.error(
                    "Experiment not found: " + id, ChaosResponseCodes.NOT_FOUND));
        }
        return ResponseEntity.ok(GenericRestResponse.success("OK", experiment, requestId));
    }

    @GetMapping("/experiments")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.EXPERIMENT_READ + "') "
            + "or hasAnyRole('VIEWER','AUDITOR','OPERATOR','ADMIN')")
    public ResponseEntity<GenericRestResponse<List<ChaosExperimentResponse>>> listExperiments() {
        String requestId = MDC.get("requestId");
        return ResponseEntity.ok(GenericRestResponse.success("OK",
                orchestrationService.listExperiments(), requestId));
    }

    @DeleteMapping("/experiments/{id}")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.EXPERIMENT_ABORT + "') "
            + "or hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<GenericRestResponse<ChaosExperimentResponse>> abortExperiment(@PathVariable String id) {
        String requestId = MDC.get("requestId");
        var result = orchestrationService.abortExperiment(id, ChaosActorResolver.currentActor());
        if (!result.aborted()) {
            return ResponseEntity.ok(GenericRestResponse.error(result.errorMessage(),
                    ChaosResponseCodes.NOT_FOUND));
        }
        return ResponseEntity.ok(GenericRestResponse.success("Experiment aborted", result.data(), requestId));
    }

    @PostMapping("/experiments/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.APPROVAL_CHECK + "') or hasRole('ADMIN')")
    public ResponseEntity<GenericRestResponse<ChaosExperimentResponse>> approveExperiment(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        if (approvalService.isEmpty()) {
            return ResponseEntity.ok(GenericRestResponse.error("Approval not enabled",
                    ApiPayloadCode.SERVICE_UNAVAILABLE));
        }
        String note = body != null ? body.getOrDefault("note", "") : "";
        ChaosExperimentResponse response = approvalService.get().approve(id, ChaosActorResolver.currentActor(), note);
        return ResponseEntity.ok(GenericRestResponse.success("Approved", response, MDC.get("requestId")));
    }

    @PostMapping("/experiments/{id}/reject")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.APPROVAL_CHECK + "') or hasRole('ADMIN')")
    public ResponseEntity<GenericRestResponse<ChaosExperimentResponse>> rejectExperiment(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        if (approvalService.isEmpty()) {
            return ResponseEntity.ok(GenericRestResponse.error("Approval not enabled",
                    ApiPayloadCode.SERVICE_UNAVAILABLE));
        }
        String note = body != null ? body.getOrDefault("note", "") : "";
        ChaosExperimentResponse response = approvalService.get().reject(id, ChaosActorResolver.currentActor(), note);
        return ResponseEntity.ok(GenericRestResponse.success("Rejected", response, MDC.get("requestId")));
    }

    @PostMapping("/emergency/disable")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.KILL_SWITCH + "') or hasRole('ADMIN')")
    public ResponseEntity<GenericRestResponse<Map<String, Boolean>>> activateKillSwitch() {
        String requestId = MDC.get("requestId");
        String actor = ChaosActorResolver.currentActor();
        safetyService.activateKillSwitch();
        auditService.auditKillSwitchActivated(actor);
        return ResponseEntity.ok(GenericRestResponse.success("Kill switch activated",
                Map.of("killSwitchActive", true), requestId));
    }

    @PostMapping("/emergency/enable")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.KILL_SWITCH + "') or hasRole('ADMIN')")
    public ResponseEntity<GenericRestResponse<Map<String, Boolean>>> deactivateKillSwitch() {
        String requestId = MDC.get("requestId");
        String actor = ChaosActorResolver.currentActor();
        safetyService.deactivateKillSwitch();
        auditService.auditKillSwitchDeactivated(actor);
        return ResponseEntity.ok(GenericRestResponse.success("Kill switch deactivated",
                Map.of("killSwitchActive", false), requestId));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_" + ChaosPermissionCodes.EXPERIMENT_READ + "') "
            + "or hasAnyRole('VIEWER','AUDITOR','OPERATOR','ADMIN')")
    public ResponseEntity<GenericRestResponse<Map<String, Object>>> getStatus() {
        String requestId = MDC.get("requestId");
        Map<String, Object> status = Map.of(
                "chaosEnabled", safetyService.isChaosEnabled(),
                "killSwitchActive", safetyService.isKillSwitchActive(),
                "allowedToRun", safetyService.isAllowedToRun()
        );
        return ResponseEntity.ok(GenericRestResponse.success("OK", status, requestId));
    }
}
