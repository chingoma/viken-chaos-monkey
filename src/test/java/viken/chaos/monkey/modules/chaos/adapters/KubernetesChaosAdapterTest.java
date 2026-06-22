package viken.chaos.monkey.modules.chaos.adapters;

import org.junit.jupiter.api.Test;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesChaosAdapterTest {

    @Test
    void blocksProdWithoutOverride() {
        ChaosK8sProperties k8s = new ChaosK8sProperties();
        ChaosSafetyProperties safety = new ChaosSafetyProperties();
        KubernetesChaosAdapter adapter = new KubernetesChaosAdapter(
                k8s, safety, "PROD", false, Optional.empty());

        var result = adapter.execute(new ChaosExperimentRequest(
                "pod-kill", "order-service", "default", Map.of("blastRadius", "1")));

        assertFalse(result.status().equalsIgnoreCase("SUCCESS"));
        assertTrue(result.message().contains("PROD"));
    }

    @Test
    void supportsPodKillAndStressTypes() {
        ChaosK8sProperties k8s = new ChaosK8sProperties();
        ChaosSafetyProperties safety = new ChaosSafetyProperties();
        KubernetesChaosAdapter adapter = new KubernetesChaosAdapter(
                k8s, safety, "DEV", false, Optional.empty());

        assertTrue(adapter.supports("pod-kill"));
        assertTrue(adapter.supports("pod-cpu-stress"));
        assertFalse(adapter.supports("http-latency"));
    }
}
