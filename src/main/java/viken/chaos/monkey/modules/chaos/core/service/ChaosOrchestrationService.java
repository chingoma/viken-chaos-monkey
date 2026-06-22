package viken.chaos.monkey.modules.chaos.core.service;

import tz.dse.trading.core.api.ApiPayloadCode;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.common.response.ChaosResponseCodes;

import java.util.List;

public interface ChaosOrchestrationService {

    ChaosExecutionResult execute(ChaosExperimentRequest request, String actor);

    ChaosExperimentResponse getExperiment(String id);

    List<ChaosExperimentResponse> listExperiments();

    AbortResult abortExperiment(String id, String actor);

    record ChaosExecutionResult(String code, ChaosExperimentResponse data, String errorMessage) {
        public static ChaosExecutionResult success(ChaosExperimentResponse data) {
            return new ChaosExecutionResult(ApiPayloadCode.SUCCESS.getCode(), data, null);
        }

        public static ChaosExecutionResult validationFailed(String code, String message) {
            return new ChaosExecutionResult(code, null, message);
        }

        public static ChaosExecutionResult error(ChaosExperimentResponse data) {
            return new ChaosExecutionResult(ChaosResponseCodes.ADAPTER_ERROR.getCode(), data, data.message());
        }
    }

    record AbortResult(boolean aborted, ChaosExperimentResponse data, String errorMessage) {
        public static AbortResult aborted(ChaosExperimentResponse data) {
            return new AbortResult(true, data, null);
        }

        public static AbortResult notFound(String message) {
            return new AbortResult(false, null, message);
        }

        public static AbortResult notRunning(String message) {
            return new AbortResult(false, null, message);
        }
    }
}
