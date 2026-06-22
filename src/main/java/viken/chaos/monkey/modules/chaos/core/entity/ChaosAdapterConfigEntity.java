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
@Table(name = "chaos_adapter_config")
public class ChaosAdapterConfigEntity extends BaseEntity {

    @Column(name = "adapter_type", nullable = false, length = 64)
    private String adapterType;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    private String configJson = "{}";

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
