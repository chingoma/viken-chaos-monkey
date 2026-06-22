package viken.chaos.monkey.common.exception;

import tz.dse.trading.core.api.ApiPayloadCode;

/**
 * Domain-level chaos safety violations mapped to ApiPayloadCode.
 */
public class ChaosBusinessException extends RuntimeException {

    private final ApiPayloadCode code;

    public ChaosBusinessException(String message, ApiPayloadCode code) {
        super(message);
        this.code = code;
    }

    public ApiPayloadCode getCode() {
        return code;
    }

    public static ChaosBusinessException domainViolation(String message) {
        return new ChaosBusinessException(message, ApiPayloadCode.DOMAIN_VIOLATION);
    }
}
