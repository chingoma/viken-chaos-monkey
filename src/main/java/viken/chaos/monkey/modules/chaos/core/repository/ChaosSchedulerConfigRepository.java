package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosSchedulerConfigEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChaosSchedulerConfigRepository extends JpaRepository<ChaosSchedulerConfigEntity, UUID> {

    List<ChaosSchedulerConfigEntity> findByEnabledTrue();

    Optional<ChaosSchedulerConfigEntity> findByName(String name);
}
