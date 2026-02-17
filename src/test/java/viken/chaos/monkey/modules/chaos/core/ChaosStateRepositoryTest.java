package viken.chaos.monkey.modules.chaos.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaosStateRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsKillSwitchAndExperimentsAcrossRepositoryInstances() {
        Path stateFile = tempDir.resolve("chaos-state.json");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ChaosSafetyProperties properties = new ChaosSafetyProperties();

        ChaosStateRepository repoOne = new ChaosStateRepository(objectMapper, stateFile.toString(), properties);
        repoOne.initialize();
        repoOne.setKillSwitchActive(true);
        repoOne.upsertExperiment(ChaosExperimentResponse.of(
                "exp-1",
                "pod-kill",
                "order-service",
                "SUCCESS",
                "done",
                Instant.now(),
                Instant.now()
        ));

        ChaosStateRepository repoTwo = new ChaosStateRepository(objectMapper, stateFile.toString(), properties);
        repoTwo.initialize();

        assertTrue(repoTwo.isKillSwitchActive());
        assertNotNull(repoTwo.getExperiment("exp-1"));
    }
}
