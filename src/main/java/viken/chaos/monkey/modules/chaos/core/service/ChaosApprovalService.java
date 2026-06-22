package viken.chaos.monkey.modules.chaos.core.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ChaosResponseCodes;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosApprovalRequestEntity;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentEntity;
import viken.chaos.monkey.modules.chaos.core.mapper.ChaosExperimentMapper;
import viken.chaos.monkey.modules.chaos.core.service.ChaosOrchestrationService.ChaosExecutionResult;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosAuditTransactionService;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosExperimentTransactionService;

import java.time.Instant;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "chaos.persistence.mode", havingValue = "db", matchIfMissing = true)
public class ChaosApprovalService {

    private final ChaosExperimentTransactionService experimentTransactionService;
    private final ChaosAuditTransactionService auditTransactionService;
    private final ChaosExperimentMapper mapper;

    public ChaosApprovalService(ChaosExperimentTransactionService experimentTransactionService,
                                ChaosAuditTransactionService auditTransactionService,
                                ChaosExperimentMapper mapper) {
        this.experimentTransactionService = experimentTransactionService;
        this.auditTransactionService = auditTransactionService;
        this.mapper = mapper;
    }

    public ChaosExecutionResult submitForApproval(ChaosExperimentRequest request, String makerId, String environment) {
        int blastRadius = request.getParamAsInt("blastRadius", 1);
        ChaosExperimentEntity entity = experimentTransactionService.createRunning(
                request, makerId, environment, null, blastRadius);
        entity.setStatus("PENDING_APPROVAL");
        auditTransactionService.createApprovalRequest(entity, makerId, "Awaiting checker approval");
        ChaosExperimentResponse response = mapper.toResponse(entity);
        return ChaosExecutionResult.validationFailed(
                ChaosResponseCodes.APPROVAL_PENDING.getCode(),
                "Experiment submitted for approval: " + entity.getUid());
    }

    public ChaosExperimentResponse approve(String experimentUid, String checkerId, String note) {
        ChaosExperimentEntity entity = experimentTransactionService.findByUid(experimentUid)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found"));
        auditTransactionService.resolveApproval(entity.getId(), checkerId, true, note);
        entity.setStatus("APPROVED");
        entity.setApprovedBy(checkerId);
        entity.setUpdatedAt(Instant.now());
        return mapper.toResponse(entity);
    }

    public ChaosExperimentResponse reject(String experimentUid, String checkerId, String note) {
        ChaosExperimentEntity entity = experimentTransactionService.findByUid(experimentUid)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found"));
        auditTransactionService.resolveApproval(entity.getId(), checkerId, false, note);
        entity.setStatus("REJECTED");
        entity.setApprovedBy(checkerId);
        entity.setUpdatedAt(Instant.now());
        return mapper.toResponse(entity);
    }
}
