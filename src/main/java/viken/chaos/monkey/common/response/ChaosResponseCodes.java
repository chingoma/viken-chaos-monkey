package viken.chaos.monkey.common.response;

import tz.dse.trading.core.api.ApiPayloadCode;

/**
 * Chaos-specific business codes mapped to 4-digit ApiPayloadCode values.
 */
public final class ChaosResponseCodes {

    private ChaosResponseCodes() {
    }

    public static final ApiPayloadCode CHAOS_DISABLED = ApiPayloadCode.DOMAIN_VIOLATION;
    public static final ApiPayloadCode KILL_SWITCH_ACTIVE = ApiPayloadCode.DOMAIN_VIOLATION;
    public static final ApiPayloadCode TARGET_EXCLUDED = ApiPayloadCode.DOMAIN_VIOLATION;
    public static final ApiPayloadCode BLAST_RADIUS_EXCEEDED = ApiPayloadCode.VALIDATION_ERROR;
    public static final ApiPayloadCode EXPERIMENT_TYPE_DISABLED = ApiPayloadCode.DOMAIN_VIOLATION;
    public static final ApiPayloadCode TIME_WINDOW_VIOLATION = ApiPayloadCode.DOMAIN_VIOLATION;
    public static final ApiPayloadCode ADAPTER_ERROR = ApiPayloadCode.INTERNAL_ERROR;
    public static final ApiPayloadCode APPROVAL_REQUIRED = ApiPayloadCode.FORBIDDEN;
    public static final ApiPayloadCode APPROVAL_PENDING = ApiPayloadCode.CONFLICT;
    public static final ApiPayloadCode NOT_FOUND = ApiPayloadCode.NOT_FOUND;
}
