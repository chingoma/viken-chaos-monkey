package viken.chaos.monkey.modules.chaos.adapters;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Real Kafka delay/DLQ injection limited to configured test topics.
 */
@Component
@Order(15)
@ConditionalOnProperty(name = "chaos.adapters.kafka.enabled", havingValue = "true")
public class KafkaChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(KafkaChaosAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Set<String> allowedTopics;
    private final String bootstrapServers;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;

    public KafkaChaosAdapter(KafkaTemplate<String, String> kafkaTemplate,
                             @Value("${chaos.adapters.kafka.allowed-topics:chaos.test}") String allowedTopics,
                             @Value("${chaos.adapters.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
                             Optional<EnvironmentPolicyService> environmentPolicyService) {
        this.kafkaTemplate = kafkaTemplate;
        this.allowedTopics = Set.of(allowedTopics.split(","));
        this.bootstrapServers = bootstrapServers;
        this.environmentPolicyService = environmentPolicyService;
    }

    @Override
    public boolean supports(String experimentType) {
        return "mq-delay".equals(experimentType) || "mq-dlq-inject".equals(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        if (environmentPolicyService.isPresent() && !environmentPolicyService.get().isRealAdapterAllowed()) {
            return ChaosExperimentResult.failed("Real Kafka adapter disabled for environment");
        }
        String topic = request.params() != null
                ? request.params().getOrDefault("topic", "chaos.test")
                : "chaos.test";
        if (!allowedTopics.contains(topic)) {
            return ChaosExperimentResult.failed("Topic not allowlisted: " + topic);
        }
        ensureTopicExists(topic);
        try {
            if ("mq-delay".equals(request.type())) {
                int delayMs = request.getParamAsInt("delayMs", 1000);
                Thread.sleep(delayMs);
                kafkaTemplate.send(topic, "chaos-delay-key", "delayed-message").get(5, TimeUnit.SECONDS);
                return ChaosExperimentResult.success("Kafka delay " + delayMs + "ms on topic " + topic);
            }
            String dlqTopic = topic + ".dlq";
            ensureTopicExists(dlqTopic);
            kafkaTemplate.send(dlqTopic, "chaos-dlq-key", "forced-dlq-message").get(5, TimeUnit.SECONDS);
            return ChaosExperimentResult.success("Message injected to DLQ topic " + dlqTopic);
        } catch (Exception e) {
            log.warn("Kafka chaos failed: {}", e.getMessage());
            return ChaosExperimentResult.failed("Kafka chaos failed: " + e.getMessage());
        }
    }

    private void ensureTopicExists(String topic) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                    .all().get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Topic create skipped for {}: {}", topic, e.getMessage());
        }
    }
}
