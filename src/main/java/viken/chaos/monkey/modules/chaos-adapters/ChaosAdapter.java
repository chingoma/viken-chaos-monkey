package viken.chaos.monkey.modules.chaos.adapters;

import viken.chaos.monkey.common.dto.ChaosExperimentRequest;

/**
 * Contract for chaos injection adapters.
 */
public interface ChaosAdapter {

    boolean supports(String experimentType);

    ChaosExperimentResult execute(ChaosExperimentRequest request);

    record ChaosExperimentResult(String status, String message) {
        public static ChaosExperimentResult success(String message) {
            return new ChaosExperimentResult("SUCCESS", message);
        }

        public static ChaosExperimentResult failed(String message) {
            return new ChaosExperimentResult("FAILED", message);
        }
    }
}
