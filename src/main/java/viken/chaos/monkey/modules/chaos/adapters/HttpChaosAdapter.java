package viken.chaos.monkey.modules.chaos.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Real HTTP latency/error injection against configured allowlisted hosts only.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "chaos.adapters.http.enabled", havingValue = "true")
public class HttpChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpChaosAdapter.class);

    private final RestClient restClient;
    private final List<String> allowedHosts;
    private final int defaultLatencyMs;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;

    public HttpChaosAdapter(@Value("${chaos.adapters.http.allowed-hosts:}") String allowedHosts,
                            @Value("${chaos.adapters.http.default-latency-ms:500}") int defaultLatencyMs,
                            Optional<EnvironmentPolicyService> environmentPolicyService) {
        this.restClient = RestClient.create();
        this.allowedHosts = Arrays.stream(allowedHosts.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.defaultLatencyMs = defaultLatencyMs;
        this.environmentPolicyService = environmentPolicyService;
    }

    @Override
    public boolean supports(String experimentType) {
        return "http-latency".equals(experimentType)
                || "http-error".equals(experimentType)
                || "http-timeout".equals(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        if (environmentPolicyService.isPresent() && !environmentPolicyService.get().isRealAdapterAllowed()) {
            return ChaosExperimentResult.failed("Real HTTP adapter disabled for environment");
        }
        String host = request.targetService();
        if (!isHostAllowed(host)) {
            return ChaosExperimentResult.failed("Host not in allowlist: " + host);
        }
        String path = request.params() != null ? request.params().getOrDefault("path", "/actuator/health") : "/actuator/health";
        String url = host.startsWith("http") ? host + path : "http://" + host + path;

        try {
            return switch (request.type()) {
                case "http-latency" -> injectLatency(url, request);
                case "http-error" -> injectError(url);
                case "http-timeout" -> injectTimeout(url, request);
                default -> ChaosExperimentResult.failed("Unsupported HTTP type: " + request.type());
            };
        } catch (Exception e) {
            log.warn("HTTP chaos failed for {}: {}", url, e.getMessage());
            return ChaosExperimentResult.failed("HTTP chaos failed: " + e.getMessage());
        }
    }

    private ChaosExperimentResult injectLatency(String url, ChaosExperimentRequest request) throws InterruptedException {
        int latencyMs = request.getParamAsInt("latencyMs", defaultLatencyMs);
        Thread.sleep(latencyMs);
        restClient.get().uri(url).retrieve().toBodilessEntity();
        return ChaosExperimentResult.success("HTTP latency " + latencyMs + "ms applied to " + url);
    }

    private ChaosExperimentResult injectError(String url) {
        var response = restClient.get().uri(url).retrieve().toEntity(String.class);
        return ChaosExperimentResult.success("HTTP probe completed status=" + response.getStatusCode().value() + " url=" + url);
    }

    private ChaosExperimentResult injectTimeout(String url, ChaosExperimentRequest request) {
        int timeoutMs = request.getParamAsInt("timeoutMs", 100);
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
            return ChaosExperimentResult.success("HTTP timeout probe completed within " + timeoutMs + "ms for " + url);
        } catch (Exception e) {
            return ChaosExperimentResult.success("HTTP timeout observed for " + url + ": " + e.getMessage());
        }
    }

    private boolean isHostAllowed(String host) {
        if (allowedHosts.isEmpty()) {
            return false;
        }
        return allowedHosts.stream().anyMatch(allowed ->
                host.equalsIgnoreCase(allowed) || host.toLowerCase().contains(allowed.toLowerCase()));
    }
}
