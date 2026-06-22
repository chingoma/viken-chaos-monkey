package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChaosExperimentRepository extends JpaRepository<ChaosExperimentEntity, UUID> {

    Optional<ChaosExperimentEntity> findByUid(String uid);

    List<ChaosExperimentEntity> findAllByOrderByCreatedAtDesc();

    long countByEnvironmentAndCreatedAtAfter(String environment, Instant since);

    long countByStatusIgnoreCase(String status);
}
