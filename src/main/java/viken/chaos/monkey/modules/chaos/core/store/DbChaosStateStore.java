package viken.chaos.monkey.modules.chaos.core.store;

import org.springframework.beans.factory.annotation.Value;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.core.mapper.ChaosExperimentMapper;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosExperimentTransactionService;

import java.util.List;

public class DbChaosStateStore implements ChaosStateStore {

    private final ChaosExperimentTransactionService transactionService;
    private final ChaosExperimentMapper mapper;
    private final String environment;

    public DbChaosStateStore(ChaosExperimentTransactionService transactionService,
                             ChaosExperimentMapper mapper,
                             @Value("${chaos.environment:LOCAL}") String environment) {
        this.transactionService = transactionService;
        this.mapper = mapper;
        this.environment = environment;
    }

    @Override
    public boolean isKillSwitchActive() {
        return transactionService.isKillSwitchActive(environment);
    }

    @Override
    public void setKillSwitchActive(boolean active) {
        transactionService.setKillSwitch(environment, active, "system");
    }

    @Override
    public ChaosExperimentResponse getExperiment(String id) {
        return transactionService.findByUid(id)
                .map(mapper::toResponse)
                .orElse(null);
    }

    @Override
    public void upsertExperiment(ChaosExperimentResponse response) {
        transactionService.findByUid(response.experimentId()).ifPresent(entity -> {
            entity.setStatus(response.status());
            entity.setResultMessage(response.message());
            entity.setStartedAt(response.startedAt());
            entity.setCompletedAt(response.completedAt());
            transactionService.updateStatus(entity, response.status(), response.message(), "adapter");
        });
    }

    @Override
    public List<ChaosExperimentResponse> listExperiments() {
        return transactionService.listAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    public long countByStatus(String status) {
        return transactionService.countRunning();
    }
}
