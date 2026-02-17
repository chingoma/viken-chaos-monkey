package viken.chaos.monkey.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * Request DTO for creating/triggering a chaos experiment.
 */
public record ChaosExperimentRequest(
        @NotBlank(message = "Experiment type is required")
        @Pattern(regexp = "pod-kill|pod-cpu-stress|pod-memory-pressure|network-latency|network-partition|http-latency|http-error|http-timeout|db-slow-query|db-connection-exhaustion|mq-delay|mq-dlq-inject",
                message = "Invalid experiment type")
        String type,

        @NotBlank(message = "Target service is required")
        String targetService,

        String namespace,

        Map<String, String> params
) {
    public String getParam(String key) {
        return params != null ? params.get(key) : null;
    }

    public String getParam(String key, String defaultValue) {
        String value = getParam(key);
        return value != null ? value : defaultValue;
    }

    public int getParamAsInt(String key, int defaultValue) {
        String value = getParam(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
