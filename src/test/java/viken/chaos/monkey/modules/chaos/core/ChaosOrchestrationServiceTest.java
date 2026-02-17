package viken.chaos.monkey.modules.chaos.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.response.ResponseCodes;
import viken.chaos.monkey.modules.chaos.adapters.ChaosAdapter;
import viken.chaos.monkey.modules.chaos.audit.ChaosAuditService;
import viken.chaos.monkey.modules.chaos.audit.ChaosMetricsService;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ChaosOrchestrationServiceTest {

    @TempDir
    Path tempDir;

    private ChaosSafetyProperties safetyProperties;
    private ChaosStateRepository stateRepository;
    private ChaosSafetyService safetyService;
    private ChaosMetricsService metricsService;
    private ChaosAuditService auditService;
    private ExecutorService executionExecutor;

    @BeforeEach
    void setUp() {
        safetyProperties = new ChaosSafetyProperties();
        safetyProperties.setEnabled(true);
        safetyProperties.setBlastRadiusMaxPercent(10);
        safetyProperties.setExcludedServices(List.of());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        stateRepository = new ChaosStateRepository(
                objectMapper,
                tempDir.resolve("chaos-state.json").toString(),
                safetyProperties
        );
        stateRepository.initialize();

        safetyService = new ChaosSafetyService(safetyProperties, stateRepository);
        metricsService = mock(ChaosMetricsService.class);
        auditService = mock(ChaosAuditService.class);
        executionExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executionExecutor.shutdownNow();
    }

    @Test
    void executeRejectsBlastRadiusAboveConfiguredLimit() {
        ChaosOrchestrationService service = new ChaosOrchestrationService(
                safetyService,
                List.of(new NoOpAdapter()),
                metricsService,
                auditService,
                stateRepository,
                executionExecutor
        );

        ChaosExperimentRequest request = new ChaosExperimentRequest(
                "pod-kill",
                "order-service",
                "default",
                Map.of("blastRadius", "30")
        );

        ChaosOrchestrationService.ChaosExecutionResult result = service.execute(request);
        assertEquals(ResponseCodes.BLAST_RADIUS_EXCEEDED, result.code());
    }

    @Test
    void abortMarksRunningExperimentAsAborted() throws Exception {
        BlockingAdapter adapter = new BlockingAdapter();
        ChaosOrchestrationService service = new ChaosOrchestrationService(
                safetyService,
                List.of(adapter),
                metricsService,
                auditService,
                stateRepository,
                executionExecutor
        );

        ChaosExperimentRequest request = new ChaosExperimentRequest(
                "pod-kill",
                "order-service",
                "default",
                Map.of("blastRadius", "1")
        );

        ChaosOrchestrationService.ChaosExecutionResult startResult = service.execute(request);
        assertNotNull(startResult.data());
        assertTrue(adapter.started.await(2, TimeUnit.SECONDS));

        String experimentId = startResult.data().experimentId();
        ChaosOrchestrationService.AbortResult abortResult = service.abortExperiment(experimentId);
        assertTrue(abortResult.aborted());
        assertEquals("ABORTED", service.getExperiment(experimentId).status());
        verify(auditService, timeout(1000)).auditExperimentFailed(
                experimentId, "pod-kill", "order-service", "Aborted by operator");
    }

    private static final class NoOpAdapter implements ChaosAdapter {
        @Override
        public boolean supports(String experimentType) {
            return "pod-kill".equals(experimentType);
        }

        @Override
        public ChaosExperimentResult execute(ChaosExperimentRequest request) {
            return ChaosExperimentResult.success("ok");
        }
    }

    private static final class BlockingAdapter implements ChaosAdapter {
        private final CountDownLatch started = new CountDownLatch(1);

        @Override
        public boolean supports(String experimentType) {
            return "pod-kill".equals(experimentType);
        }

        @Override
        public ChaosExperimentResult execute(ChaosExperimentRequest request) {
            started.countDown();
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ChaosExperimentResult.failed("interrupted");
            }
            return ChaosExperimentResult.success("done");
        }
    }
}
