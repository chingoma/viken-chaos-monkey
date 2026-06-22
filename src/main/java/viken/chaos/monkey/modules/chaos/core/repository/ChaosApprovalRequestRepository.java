package viken.chaos.monkey.modules.chaos.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosApprovalRequestEntity;

import java.util.Optional;
import java.util.UUID;

public interface ChaosApprovalRequestRepository extends JpaRepository<ChaosApprovalRequestEntity, UUID> {

    Optional<ChaosApprovalRequestEntity> findByExperimentId(UUID experimentId);
}
