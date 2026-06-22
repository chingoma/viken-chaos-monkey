package viken.chaos.monkey.modules.chaos.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.dse.trading.core.response.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chaos_experiment_audit")
public class ChaosExperimentAuditEntity extends BaseEntity {

    @Column(name = "experiment_id", columnDefinition = "uuid")
    private UUID experimentId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 128)
    private String actor;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
