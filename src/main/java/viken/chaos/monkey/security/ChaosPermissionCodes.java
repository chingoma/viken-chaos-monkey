package viken.chaos.monkey.security;

/**
 * IAM permission codes for chaos operations (raw format for hasPermission).
 */
public final class ChaosPermissionCodes {

    private ChaosPermissionCodes() {
    }

    public static final String EXPERIMENT_READ = "chaos:experiment:read";
    public static final String EXPERIMENT_CREATE = "chaos:experiment:create";
    public static final String EXPERIMENT_ABORT = "chaos:experiment:abort";
    public static final String KILL_SWITCH = "chaos:admin:kill-switch";
    public static final String APPROVAL_CHECK = "chaos:approval:check";
    public static final String POLICY_READ = "chaos:policy:read";
    public static final String POLICY_WRITE = "chaos:policy:write";
    public static final String SCHEDULER_MANAGE = "chaos:scheduler:manage";
}
