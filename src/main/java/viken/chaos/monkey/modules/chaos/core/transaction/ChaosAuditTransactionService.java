package viken.chaos.monkey.modules.chaos.core.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosApprovalRequestEntity;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentAuditEntity;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentEntity;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosApprovalRequestRepository;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosExperimentAuditRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChaosAuditTransactionService {

    private final ChaosExperimentAuditRepository auditRepository;
    private final ChaosApprovalRequestRepository approvalRepository;
    private final ObjectMapper objectMapper;

    public ChaosAuditTransactionService(ChaosExperimentAuditRepository auditRepository,
                                        ChaosApprovalRequestRepository approvalRepository,
                                        ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.approvalRepository = approvalRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChaosExperimentAuditEntity persistAudit(String action, UUID experimentId, String actor,
                                                   String requestId, Map<String, Object> payload) {
        ChaosExperimentAuditEntity audit = new ChaosExperimentAuditEntity();
        audit.setAction(action);
        audit.setExperimentId(experimentId);
        audit.setActor(actor);
        audit.setRequestId(requestId);
        audit.setOccurredAt(Instant.now());
        try {
            audit.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            audit.setPayloadJson("{}");
        }
        return auditRepository.save(audit);
    }

    @Transactional
    public ChaosApprovalRequestEntity createApprovalRequest(ChaosExperimentEntity experiment, String makerId,
                                                            String makerNote) {
        ChaosApprovalRequestEntity approval = new ChaosApprovalRequestEntity();
        approval.setExperimentId(experiment.getId());
        approval.setStatus("PENDING");
        approval.setMakerId(makerId);
        approval.setMakerNote(makerNote);
        approval.setRequestedAt(Instant.now());
        experiment.setStatus("PENDING_APPROVAL");
        return approvalRepository.save(approval);
    }

    @Transactional
    public Optional<ChaosApprovalRequestEntity> resolveApproval(UUID experimentId, String checkerId,
                                                                boolean approved, String checkerNote) {
        return approvalRepository.findByExperimentId(experimentId).map(approval -> {
            approval.setStatus(approved ? "APPROVED" : "REJECTED");
            approval.setCheckerId(checkerId);
            approval.setCheckerNote(checkerNote);
            approval.setResolvedAt(Instant.now());
            approval.setUpdatedAt(Instant.now());
            return approvalRepository.save(approval);
        });
    }

    @Transactional(readOnly = true)
    public Optional<ChaosApprovalRequestEntity> findApproval(UUID experimentId) {
        return approvalRepository.findByExperimentId(experimentId);
    }
}
