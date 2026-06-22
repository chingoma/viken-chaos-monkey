package viken.chaos.monkey.modules.chaos.core.store;

import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.core.ChaosStateRepository;

import java.util.List;

public class FileChaosStateStore implements ChaosStateStore {

    private final ChaosStateRepository repository;

    public FileChaosStateStore(ChaosStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isKillSwitchActive() {
        return repository.isKillSwitchActive();
    }

    @Override
    public void setKillSwitchActive(boolean active) {
        repository.setKillSwitchActive(active);
    }

    @Override
    public ChaosExperimentResponse getExperiment(String id) {
        return repository.getExperiment(id);
    }

    @Override
    public void upsertExperiment(ChaosExperimentResponse response) {
        repository.upsertExperiment(response);
    }

    @Override
    public List<ChaosExperimentResponse> listExperiments() {
        return repository.listExperiments();
    }

    @Override
    public long countByStatus(String status) {
        return repository.countByStatus(status);
    }
}
