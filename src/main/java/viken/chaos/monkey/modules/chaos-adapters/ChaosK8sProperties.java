package viken.chaos.monkey.modules.chaos.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chaos.k8s")
public class ChaosK8sProperties {

    private boolean enabled = false;
    private String namespace = "default";
    private boolean inCluster = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isInCluster() {
        return inCluster;
    }

    public void setInCluster(boolean inCluster) {
        this.inCluster = inCluster;
    }
}
