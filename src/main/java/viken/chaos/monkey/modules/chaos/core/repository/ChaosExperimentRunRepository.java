package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentRunEntity;

import java.util.UUID;

public interface ChaosExperimentRunRepository extends JpaRepository<ChaosExperimentRunEntity, UUID> {

    long countByExperimentId(UUID experimentId);
}
