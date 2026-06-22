package viken.chaos.monkey.modules.chaos.core.store;

import org.springframework.stereotype.Repository;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosExperimentRepository;

import java.time.Instant;

@Repository
public class ChaosExperimentCounter {

    private final ChaosExperimentRepository repository;

    public ChaosExperimentCounter(ChaosExperimentRepository repository) {
        this.repository = repository;
    }

    public long countSince(String environment, Instant since) {
        return repository.countByEnvironmentAndCreatedAtAfter(environment, since);
    }
}
