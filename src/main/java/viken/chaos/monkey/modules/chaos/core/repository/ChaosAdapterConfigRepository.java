package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosAdapterConfigEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChaosAdapterConfigRepository extends JpaRepository<ChaosAdapterConfigEntity, UUID> {

    Optional<ChaosAdapterConfigEntity> findByAdapterTypeAndEnvironmentIgnoreCase(String adapterType, String environment);

    List<ChaosAdapterConfigEntity> findByEnvironmentIgnoreCase(String environment);
}
