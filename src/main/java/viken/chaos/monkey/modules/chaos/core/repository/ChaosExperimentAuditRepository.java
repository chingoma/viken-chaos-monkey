package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentAuditEntity;

import java.util.UUID;

public interface ChaosExperimentAuditRepository extends JpaRepository<ChaosExperimentAuditEntity, UUID> {
}
