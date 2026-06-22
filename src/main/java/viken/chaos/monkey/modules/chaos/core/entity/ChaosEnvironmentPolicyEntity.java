package viken.chaos.monkey.modules.chaos.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.dse.trading.core.response.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "chaos_environment_policy")
public class ChaosEnvironmentPolicyEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String environment;

    @Column(name = "allow_real_adapters", nullable = false)
    private boolean allowRealAdapters;

    @Column(name = "allow_scheduler", nullable = false)
    private boolean allowScheduler;

    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval;

    @Column(name = "max_daily_experiments", nullable = false)
    private int maxDailyExperiments = 10;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_namespaces", nullable = false, columnDefinition = "jsonb")
    private String allowedNamespaces = "[]";

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
