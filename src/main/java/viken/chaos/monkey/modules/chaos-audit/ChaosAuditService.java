package viken.chaos.monkey.modules.chaos.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;

import java.time.Instant;
import java.util.Map;

/**
 * Async audit logging for chaos experiments. Non-blocking per enterprise guidelines.
 */
@Service
public class ChaosAuditService {

    private static final Logger log = LoggerFactory.getLogger(ChaosAuditService.class);
    private static final String AUDIT_EVENT = "CHAOS_EXPERIMENT";

    private final ObjectMapper objectMapper;

    public ChaosAuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Async("auditExecutor")
    public void auditExperimentTriggered(ChaosExperimentRequest request, String experimentId, String actor) {
        publishAuditEvent("TRIGGERED", experimentId, request.type(), request.targetService(),
                Map.of("requestId", nullToEmpty(MDC.get("requestId")), "actor", nullToEmpty(actor)));
    }

    @Async("auditExecutor")
    public void auditExperimentCompleted(ChaosExperimentResponse response) {
        publishAuditEvent("COMPLETED", response.experimentId(), response.type(), response.targetService(),
                Map.of("status", response.status(), "message", nullToEmpty(response.message())));
    }

    @Async("auditExecutor")
    public void auditExperimentFailed(String experimentId, String type, String targetService, String reason) {
        publishAuditEvent("FAILED", experimentId, type, targetService,
                Map.of("reason", nullToEmpty(reason)));
    }

    @Async("auditExecutor")
    public void auditKillSwitchActivated(String actor) {
        publishAuditEvent("KILL_SWITCH_ACTIVATED", null, null, null,
                Map.of("actor", nullToEmpty(actor), "requestId", nullToEmpty(MDC.get("requestId"))));
    }

    @Async("auditExecutor")
    public void auditKillSwitchDeactivated(String actor) {
        publishAuditEvent("KILL_SWITCH_DEACTIVATED", null, null, null,
                Map.of("actor", nullToEmpty(actor), "requestId", nullToEmpty(MDC.get("requestId"))));
    }

    private void publishAuditEvent(String action, String experimentId, String type, String targetService,
                                   Map<String, String> payload) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", AUDIT_EVENT,
                    "action", action,
                    "timestamp", Instant.now().toString(),
                    "requestId", nullToEmpty(MDC.get("requestId")),
                    "experimentId", nullToEmpty(experimentId),
                    "type", nullToEmpty(type),
                    "targetService", nullToEmpty(targetService),
                    "payload", payload,
                    "direction", "INBOUND"
            );
            String json = objectMapper.writeValueAsString(event);
            log.info("AUDIT: {}", json);
            // In production: publish to Kafka/Redis Streams topic audit.chaos.experiments
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit event: {}", e.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
