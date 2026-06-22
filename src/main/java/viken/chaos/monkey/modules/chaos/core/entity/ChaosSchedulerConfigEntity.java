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
@Table(name = "chaos_scheduler_config")
public class ChaosSchedulerConfigEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "cron_expression", nullable = false, length = 64)
    private String cronExpression;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "experiment_type", nullable = false, length = 64)
    private String experimentType = "pod-kill";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_services", nullable = false, columnDefinition = "jsonb")
    private String targetServices = "[]";

    @Column(name = "default_blast_radius", nullable = false)
    private int defaultBlastRadius = 5;

    @Column(nullable = false, length = 32)
    private String environment = "DEV";

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "created_by", length = 128)
    private String createdBy;
}
