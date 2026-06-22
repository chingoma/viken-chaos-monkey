package viken.chaos.monkey.modules.chaos.core.store;

import viken.chaos.monkey.common.dto.ChaosExperimentResponse;

import java.util.List;

/**
 * Abstraction over file or database experiment state.
 */
public interface ChaosStateStore {

    boolean isKillSwitchActive();

    void setKillSwitchActive(boolean active);

    ChaosExperimentResponse getExperiment(String id);

    void upsertExperiment(ChaosExperimentResponse response);

    List<ChaosExperimentResponse> listExperiments();

    long countByStatus(String status);
}
