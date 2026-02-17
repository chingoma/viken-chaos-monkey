package viken.chaos.monkey.common.dto;

import java.time.Instant;

/**
 * Response DTO for chaos experiment execution.
 */
public record ChaosExperimentResponse(
        String experimentId,
        String type,
        String targetService,
        String status,
        String message,
        Instant startedAt,
        Instant completedAt
) {
    public static ChaosExperimentResponse of(String experimentId, String type, String targetService,
                                             String status, String message, Instant startedAt, Instant completedAt) {
        return new ChaosExperimentResponse(
                experimentId,
                type,
                targetService,
                status,
                message,
                startedAt,
                completedAt
        );
    }
}
