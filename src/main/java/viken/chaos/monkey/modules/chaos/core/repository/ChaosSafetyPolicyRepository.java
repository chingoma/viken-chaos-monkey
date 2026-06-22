package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosSafetyPolicyEntity;

import java.util.Optional;
import java.util.UUID;

public interface ChaosSafetyPolicyRepository extends JpaRepository<ChaosSafetyPolicyEntity, UUID> {

    Optional<ChaosSafetyPolicyEntity> findByEnvironmentIgnoreCase(String environment);
}
