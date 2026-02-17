package viken.chaos.monkey.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chaos.security")
public class ChaosSecurityProperties {

    private boolean enabled = true;
    private UserCredentials viewer = new UserCredentials("chaos-viewer", "change-me-viewer");
    private UserCredentials operator = new UserCredentials("chaos-operator", "change-me-operator");
    private UserCredentials admin = new UserCredentials("chaos-admin", "change-me-admin");
    private UserCredentials auditor = new UserCredentials("chaos-auditor", "change-me-auditor");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UserCredentials getViewer() {
        return viewer;
    }

    public void setViewer(UserCredentials viewer) {
        this.viewer = viewer;
    }

    public UserCredentials getOperator() {
        return operator;
    }

    public void setOperator(UserCredentials operator) {
        this.operator = operator;
    }

    public UserCredentials getAdmin() {
        return admin;
    }

    public void setAdmin(UserCredentials admin) {
        this.admin = admin;
    }

    public UserCredentials getAuditor() {
        return auditor;
    }

    public void setAuditor(UserCredentials auditor) {
        this.auditor = auditor;
    }

    public static class UserCredentials {
        private String username;
        private String password;

        public UserCredentials() {
        }

        public UserCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
