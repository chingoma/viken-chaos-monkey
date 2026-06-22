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
@Table(name = "chaos_approval_request")
public class ChaosApprovalRequestEntity extends BaseEntity {

    @Column(name = "experiment_id", nullable = false, columnDefinition = "uuid")
    private UUID experimentId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "maker_id", nullable = false, length = 128)
    private String makerId;

    @Column(name = "checker_id", length = 128)
    private String checkerId;

    @Column(name = "maker_note", columnDefinition = "text")
    private String makerNote;

    @Column(name = "checker_note", columnDefinition = "text")
    private String checkerNote;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
