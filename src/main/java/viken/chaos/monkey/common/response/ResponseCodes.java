package viken.chaos.monkey.common.response;

/**
 * Centralized response codes per enterprise guidelines.
 * In production, these would be fetched from config-service.
 */
public final class ResponseCodes {

    private ResponseCodes() {
    }

    public static final String SUCCESS = "00";
    public static final String VALIDATION_ERROR = "01";
    public static final String CHAOS_DISABLED = "02";
    public static final String KILL_SWITCH_ACTIVE = "03";
    public static final String TARGET_EXCLUDED = "04";
    public static final String BLAST_RADIUS_EXCEEDED = "05";
    public static final String EXPERIMENT_TYPE_DISABLED = "06";
    public static final String TIME_WINDOW_VIOLATION = "07";
    public static final String ADAPTER_ERROR = "08";
    public static final String INTERNAL_ERROR = "99";
}
