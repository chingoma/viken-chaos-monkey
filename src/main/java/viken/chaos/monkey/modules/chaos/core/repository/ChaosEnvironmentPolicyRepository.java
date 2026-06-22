package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosEnvironmentPolicyEntity;

import java.util.Optional;
import java.util.UUID;

public interface ChaosEnvironmentPolicyRepository extends JpaRepository<ChaosEnvironmentPolicyEntity, UUID> {

    Optional<ChaosEnvironmentPolicyEntity> findByEnvironmentIgnoreCase(String environment);
}
