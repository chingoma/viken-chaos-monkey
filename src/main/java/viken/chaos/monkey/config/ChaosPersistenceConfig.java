package viken.chaos.monkey.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import viken.chaos.monkey.modules.chaos.core.ChaosStateRepository;
import viken.chaos.monkey.modules.chaos.core.mapper.ChaosExperimentMapper;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;
import viken.chaos.monkey.modules.chaos.core.store.DbChaosStateStore;
import viken.chaos.monkey.modules.chaos.core.store.FileChaosStateStore;
import viken.chaos.monkey.modules.chaos.core.transaction.ChaosExperimentTransactionService;

@Configuration
public class ChaosPersistenceConfig {

    @Bean
    @ConditionalOnProperty(name = "chaos.persistence.mode", havingValue = "file")
    @Primary
    ChaosStateStore fileChaosStateStore(ChaosStateRepository repository) {
        return new FileChaosStateStore(repository);
    }

    @Bean
    @ConditionalOnProperty(name = "chaos.persistence.mode", havingValue = "db", matchIfMissing = true)
    @Primary
    ChaosStateStore dbChaosStateStore(ChaosExperimentTransactionService transactionService,
                                      ChaosExperimentMapper mapper,
                                      @Value("${chaos.environment:LOCAL}") String environment) {
        return new DbChaosStateStore(transactionService, mapper, environment);
    }
}
