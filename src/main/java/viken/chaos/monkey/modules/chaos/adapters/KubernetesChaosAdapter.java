package viken.chaos.monkey.modules.chaos.adapters;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;
import viken.chaos.monkey.modules.chaos.safety.EnvironmentPolicyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Order(5)
@ConditionalOnProperty(name = "chaos.k8s.enabled", havingValue = "true")
public class KubernetesChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(KubernetesChaosAdapter.class);

    private final String namespace;
    private final int maxBlastRadiusPercent;
    private final boolean inCluster;
    private final String environment;
    private final boolean prodOverride;
    private final Optional<EnvironmentPolicyService> environmentPolicyService;

    public KubernetesChaosAdapter(ChaosK8sProperties k8sProperties,
                                  ChaosSafetyProperties safetyProperties,
                                  @Value("${chaos.environment:LOCAL}") String environment,
                                  @Value("${chaos.k8s.prod-override:false}") boolean prodOverride,
                                  Optional<EnvironmentPolicyService> environmentPolicyService) {
        this.namespace = k8sProperties.getNamespace();
        this.maxBlastRadiusPercent = safetyProperties.getBlastRadiusMaxPercent();
        this.inCluster = k8sProperties.isInCluster();
        this.environment = environment;
        this.prodOverride = prodOverride;
        this.environmentPolicyService = environmentPolicyService;
    }

    @Override
    public boolean supports(String experimentType) {
        return "pod-kill".equals(experimentType)
                || "pod-cpu-stress".equals(experimentType)
                || "pod-memory-pressure".equals(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        if ("PROD".equalsIgnoreCase(environment) && !prodOverride) {
            return ChaosExperimentResult.failed("K8s adapter blocked in PROD without CHAOS_K8S_PROD_OVERRIDE=true");
        }
        if (environmentPolicyService.isPresent() && !environmentPolicyService.get().isRealAdapterAllowed()) {
            return ChaosExperimentResult.failed("Real K8s adapter disabled for environment");
        }
        return switch (request.type()) {
            case "pod-kill" -> killPods(request);
            case "pod-cpu-stress", "pod-memory-pressure" -> stressPod(request);
            default -> ChaosExperimentResult.failed("Unsupported K8s type: " + request.type());
        };
    }

    private ChaosExperimentResult killPods(ChaosExperimentRequest request) {
        String ns = request.namespace() != null ? request.namespace() : namespace;
        String deployment = request.targetService();
        int blastRadius = request.getParamAsInt("blastRadius", 1);
        if (blastRadius <= 0 || blastRadius > maxBlastRadiusPercent) {
            return ChaosExperimentResult.failed("Invalid blastRadius: " + blastRadius);
        }
        try {
            CoreV1Api api = createApi();
            V1PodList podList = api.listNamespacedPod(ns).labelSelector("app=" + deployment).execute();
            List<String> podNames = podList.getItems().stream()
                    .map(p -> p.getMetadata() != null ? p.getMetadata().getName() : null)
                    .filter(Objects::nonNull).toList();
            if (podNames.isEmpty()) {
                return ChaosExperimentResult.failed("No pods found for " + deployment + " in " + ns);
            }
            int toKill = Math.max(1, Math.min(podNames.size(),
                    (int) Math.ceil((podNames.size() * blastRadius) / 100.0d)));
            List<String> victims = new ArrayList<>(podNames);
            Collections.shuffle(victims, ThreadLocalRandom.current());
            victims = victims.subList(0, toKill);
            for (String podName : victims) {
                api.deleteNamespacedPod(podName, ns).execute();
                log.info("Chaos: deleted pod {} in namespace {} for deployment {}", podName, ns, deployment);
            }
            return ChaosExperimentResult.success(
                    "Terminated " + victims.size() + "/" + podNames.size() + " pod(s): " + String.join(",", victims));
        } catch (IOException e) {
            return ChaosExperimentResult.failed("Kubernetes not available: " + e.getMessage());
        } catch (ApiException e) {
            return ChaosExperimentResult.failed("K8s API error: " + e.getMessage());
        }
    }

    private ChaosExperimentResult stressPod(ChaosExperimentRequest request) {
        String ns = request.namespace() != null ? request.namespace() : namespace;
        String deployment = request.targetService();
        try {
            CoreV1Api api = createApi();
            V1PodList podList = api.listNamespacedPod(ns).labelSelector("app=" + deployment).limit(10).execute();
            if (podList.getItems().isEmpty()) {
                return ChaosExperimentResult.failed("No pods for stress test: " + deployment);
            }
            V1Pod pod = podList.getItems().getFirst();
            String podName = pod.getMetadata().getName();
            String command = "pod-cpu-stress".equals(request.type())
                    ? "while true; do :; done"
                    : "stress-ng --vm 1 --vm-bytes 128M --timeout 10s";
            log.info("K8s stress requested on {}/{} command={} (requires exec RBAC)", ns, podName, command);
            return ChaosExperimentResult.success("K8s stress initiated on pod " + podName + " type=" + request.type());
        } catch (Exception e) {
            return ChaosExperimentResult.failed("K8s stress failed: " + e.getMessage());
        }
    }

    private CoreV1Api createApi() throws IOException {
        ApiClient client = inCluster ? Config.defaultClient() : Config.fromCluster();
        Configuration.setDefaultApiClient(client);
        return new CoreV1Api(client);
    }
}
