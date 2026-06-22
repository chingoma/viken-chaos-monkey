package viken.chaos.monkey.modules.chaos.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosAuditTransactionService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChaosAuditService {

    private static final Logger log = LoggerFactory.getLogger(ChaosAuditService.class);
    private static final String AUDIT_EVENT = "CHAOS_EXPERIMENT";

    private final ObjectMapper objectMapper;
    private final Optional<ChaosAuditTransactionService> auditTransactionService;
    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
    private final String auditTopic;
    private final boolean kafkaEnabled;

    public ChaosAuditService(ObjectMapper objectMapper,
                             Optional<ChaosAuditTransactionService> auditTransactionService,
                             Optional<KafkaTemplate<String, String>> kafkaTemplate,
                             @Value("${kafka.topic.chaosAudit:audit.chaos.experiments}") String auditTopic,
                             @Value("${app.kafka.enabled:false}") boolean kafkaEnabled) {
        this.objectMapper = objectMapper;
        this.auditTransactionService = auditTransactionService;
        this.kafkaTemplate = kafkaTemplate;
        this.auditTopic = auditTopic;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Async("auditExecutor")
    public void auditExperimentTriggered(ChaosExperimentRequest request, String experimentId, String actor) {
        publishAuditEvent("TRIGGERED", experimentId, request.type(), request.targetService(),
                Map.of("requestId", nullToEmpty(MDC.get("requestId")), "actor", nullToEmpty(actor)), actor);
    }

    @Async("auditExecutor")
    public void auditExperimentCompleted(ChaosExperimentResponse response) {
        publishAuditEvent("COMPLETED", response.experimentId(), response.type(), response.targetService(),
                Map.of("status", response.status(), "message", nullToEmpty(response.message())), "system");
    }

    @Async("auditExecutor")
    public void auditExperimentFailed(String experimentId, String type, String targetService, String reason) {
        publishAuditEvent("FAILED", experimentId, type, targetService,
                Map.of("reason", nullToEmpty(reason)), "system");
    }

    @Async("auditExecutor")
    public void auditKillSwitchActivated(String actor) {
        publishAuditEvent("KILL_SWITCH_ACTIVATED", null, null, null,
                Map.of("actor", nullToEmpty(actor), "requestId", nullToEmpty(MDC.get("requestId"))), actor);
    }

    @Async("auditExecutor")
    public void auditKillSwitchDeactivated(String actor) {
        publishAuditEvent("KILL_SWITCH_DEACTIVATED", null, null, null,
                Map.of("actor", nullToEmpty(actor), "requestId", nullToEmpty(MDC.get("requestId"))), actor);
    }

    private void publishAuditEvent(String action, String experimentId, String type, String targetService,
                                   Map<String, String> payload, String actor) {
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

            auditTransactionService.ifPresent(tx -> tx.persistAudit(
                    action,
                    experimentId != null ? parseUuid(experimentId) : null,
                    actor,
                    MDC.get("requestId"),
                    event
            ));

            if (kafkaEnabled) {
                kafkaTemplate.ifPresent(template ->
                        template.send(auditTopic, nullToEmpty(experimentId), json));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit event: {}", e.getMessage());
        }
    }

    private static UUID parseUuid(String experimentId) {
        try {
            return UUID.fromString(experimentId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
