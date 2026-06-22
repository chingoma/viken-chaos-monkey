package viken.chaos.monkey.modules.chaos.core.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosSchedulerConfigEntity;
import viken.chaos.monkey.modules.chaos.core.repository.ChaosSchedulerConfigRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ChaosSchedulerTransactionService {

    private final ChaosSchedulerConfigRepository repository;

    public ChaosSchedulerTransactionService(ChaosSchedulerConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChaosSchedulerConfigEntity> findEnabled() {
        return repository.findByEnabledTrue();
    }

    @Transactional
    public void recordRun(ChaosSchedulerConfigEntity config, String status, Instant nextRun) {
        config.setLastRunAt(Instant.now());
        config.setLastStatus(status);
        config.setNextRunAt(nextRun);
        config.setUpdatedAt(Instant.now());
        repository.save(config);
    }
}
