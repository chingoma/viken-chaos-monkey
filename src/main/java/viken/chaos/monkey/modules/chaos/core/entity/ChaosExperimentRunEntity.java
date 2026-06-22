package viken.chaos.monkey.modules.chaos.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tz.dse.trading.core.response.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chaos_experiment_run")
public class ChaosExperimentRunEntity extends BaseEntity {

    @Column(name = "experiment_id", nullable = false, columnDefinition = "uuid")
    private UUID experimentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", insertable = false, updatable = false)
    private ChaosExperimentEntity experiment;

    @Column(name = "run_number", nullable = false)
    private int runNumber;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "adapter_used", length = 64)
    private String adapterUsed;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;
}
