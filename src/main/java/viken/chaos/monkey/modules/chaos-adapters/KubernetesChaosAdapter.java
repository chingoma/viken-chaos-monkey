package viken.chaos.monkey.modules.chaos.adapters;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kubernetes adapter for pod-kill chaos. Enable with chaos.k8s.enabled=true.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "chaos.k8s.enabled", havingValue = "true")
public class KubernetesChaosAdapter implements ChaosAdapter {

    private static final Logger log = LoggerFactory.getLogger(KubernetesChaosAdapter.class);

    private final String namespace;
    private final int maxBlastRadiusPercent;

    public KubernetesChaosAdapter(ChaosK8sProperties k8sProperties,
                                  viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties safetyProperties) {
        this.namespace = k8sProperties.getNamespace();
        this.maxBlastRadiusPercent = safetyProperties.getBlastRadiusMaxPercent();
    }

    @Override
    public boolean supports(String experimentType) {
        return "pod-kill".equals(experimentType);
    }

    @Override
    public ChaosExperimentResult execute(ChaosExperimentRequest request) {
        if (!"pod-kill".equals(request.type())) {
            return ChaosExperimentResult.failed("Unsupported type: " + request.type());
        }

        String ns = request.namespace() != null ? request.namespace() : namespace;
        String deployment = request.targetService();
        int blastRadius = request.getParamAsInt("blastRadius", 1);
        if (blastRadius <= 0) {
            return ChaosExperimentResult.failed("Invalid blastRadius: " + blastRadius);
        }
        if (blastRadius > maxBlastRadiusPercent) {
            return ChaosExperimentResult.failed("Requested blastRadius " + blastRadius
                    + " exceeds max allowed " + maxBlastRadiusPercent);
        }

        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api(client);

            // listNamespacedPod(namespace, pretty, allowWatchBookmarks, _continue, fieldSelector,
            //   labelSelector, limit, resourceVersion, resourceVersionMatch, timeoutSeconds, watch)
            V1PodList podList = api.listNamespacedPod(
                    ns,
                    null,
                    null,
                    null,
                    null,
                    "app=" + deployment,
                    100,
                    null,
                    null,
                    30,
                    false
            );

            List<V1Pod> pods = podList.getItems();
            if (pods == null || pods.isEmpty()) {
                return ChaosExperimentResult.failed("No pods found for " + deployment + " in " + ns);
            }

            List<String> podNames = pods.stream()
                    .map(p -> p.getMetadata() != null ? p.getMetadata().getName() : null)
                    .filter(Objects::nonNull)
                    .toList();
            if (podNames.isEmpty()) {
                return ChaosExperimentResult.failed("No pod names resolved for " + deployment + " in " + ns);
            }

            int toKill = (int) Math.ceil((podNames.size() * blastRadius) / 100.0d);
            toKill = Math.max(1, Math.min(toKill, podNames.size()));

            List<String> victims = new ArrayList<>(podNames);
            Collections.shuffle(victims, ThreadLocalRandom.current());
            victims = victims.subList(0, toKill);

            for (String podName : victims) {
                api.deleteNamespacedPod(
                        podName,
                        ns,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new V1DeleteOptions()
                );
                log.info("Chaos: deleted pod {} in namespace {} for deployment {}", podName, ns, deployment);
            }

            return ChaosExperimentResult.success(
                    "Terminated " + victims.size() + "/" + podNames.size() + " pod(s): " + String.join(",", victims));
        } catch (IOException e) {
            log.warn("K8s client not configured (not in cluster?): {}", e.getMessage());
            return ChaosExperimentResult.failed("Kubernetes not available: " + e.getMessage());
        } catch (ApiException e) {
            log.error("K8s API error: {}", e.getResponseBody());
            return ChaosExperimentResult.failed("K8s API error: " + e.getMessage());
        }
    }
}
