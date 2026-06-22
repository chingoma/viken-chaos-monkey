package viken.chaos.monkey.modules.chaos.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;

import java.util.Set;

/**
 * Simulated adapter for development/testing when K8s is not available.
 * Logs the intended chaos without actual injection.
 */
@Component
@Order(100)
public class SimulatedChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedChaosAdapter.class);

    private static final Set<String> SUPPORTED = Set.of(
            "pod-kill", "pod-cpu-stress", "pod-memory-pressure",
            "network-latency", "network-partition",
            "http-latency", "http-error", "http-timeout",
            "db-slow-query", "db-connection-exhaustion",
            "mq-delay", "mq-dlq-inject"
    );

    @Override
    public boolean supports(String experimentType) {
        return SUPPORTED.contains(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        log.info("[SIMULATED] Chaos experiment: type={}, target={}, namespace={}, params={}",
                request.type(), request.targetService(), request.namespace(), request.params());

        return ChaosExperimentResult.success(
                "Simulated: " + request.type() + " on " + request.targetService());
    }
}
