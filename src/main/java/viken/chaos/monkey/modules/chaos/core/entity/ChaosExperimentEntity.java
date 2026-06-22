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

@Getter
@Setter
@Entity
@Table(name = "chaos_experiment")
public class ChaosExperimentEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "target_service", nullable = false, length = 128)
    private String targetService;

    @Column(length = 128)
    private String namespace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", nullable = false, columnDefinition = "jsonb")
    private String paramsJson = "{}";

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "requested_by", length = 128)
    private String requestedBy;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "executed_by", length = 128)
    private String executedBy;

    @Column(name = "blast_radius_percent", nullable = false)
    private int blastRadiusPercent = 1;

    @Column(nullable = false, length = 32)
    private String environment = "LOCAL";

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "result_message", columnDefinition = "text")
    private String resultMessage;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;
}
