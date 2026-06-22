package viken.chaos.monkey.modules.chaos.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.dse.trading.core.response.BaseEntity;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "chaos_safety_policy")
public class ChaosSafetyPolicyEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String environment;

    @Column(name = "chaos_enabled", nullable = false)
    private boolean chaosEnabled;

    @Column(name = "kill_switch_active", nullable = false)
    private boolean killSwitchActive;

    @Column(name = "blast_radius_max_percent", nullable = false)
    private int blastRadiusMaxPercent = 10;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "excluded_services", nullable = false, columnDefinition = "jsonb")
    private String excludedServices = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_types", nullable = false, columnDefinition = "jsonb")
    private String enabledTypes = "[]";

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
