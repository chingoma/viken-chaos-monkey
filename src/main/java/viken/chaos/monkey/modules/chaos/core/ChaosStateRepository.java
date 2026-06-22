package viken.chaos.monkey.modules.chaos.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists runtime chaos state (kill switch + experiments) to disk.
 */
@Service
public class ChaosStateRepository {

    private static final Logger log = LoggerFactory.getLogger(ChaosStateRepository.class);

    private final ObjectMapper objectMapper;
    private final Path stateFile;
    private final boolean defaultKillSwitchActive;
    private final Map<String, ChaosExperimentResponse> experiments = new ConcurrentHashMap<>();
    private volatile boolean killSwitchActive;

    public ChaosStateRepository(ObjectMapper objectMapper,
                                @Value("${chaos.state.file:./data/chaos-state.json}") String stateFilePath,
                                ChaosSafetyProperties safetyProperties) {
        this.objectMapper = objectMapper;
        this.stateFile = Path.of(stateFilePath).toAbsolutePath().normalize();
        this.defaultKillSwitchActive = safetyProperties.isKillSwitchActive();
        this.killSwitchActive = this.defaultKillSwitchActive;
    }

    @PostConstruct
    public void initialize() {
        loadState();
    }

    public boolean isKillSwitchActive() {
        return killSwitchActive;
    }

    public synchronized void setKillSwitchActive(boolean active) {
        this.killSwitchActive = active;
        persistState();
    }

    public ChaosExperimentResponse getExperiment(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return experiments.get(id);
    }

    public synchronized void upsertExperiment(ChaosExperimentResponse response) {
        if (response == null || response.experimentId() == null || response.experimentId().isBlank()) {
            return;
        }
        experiments.put(response.experimentId(), response);
        persistState();
    }

    public List<ChaosExperimentResponse> listExperiments() {
        return experiments.values().stream()
                .sorted(Comparator.comparing(
                        ChaosExperimentResponse::startedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public long countByStatus(String status) {
        if (status == null || status.isBlank()) {
            return 0;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return experiments.values().stream()
                .filter(e -> e.status() != null && normalized.equals(e.status().toUpperCase(Locale.ROOT)))
                .count();
    }

    private synchronized void loadState() {
        if (!Files.exists(stateFile)) {
            persistState();
            return;
        }

        try {
            ChaosStateSnapshot snapshot = objectMapper.readValue(stateFile.toFile(), ChaosStateSnapshot.class);
            experiments.clear();
            if (snapshot.experiments != null) {
                for (ChaosExperimentResponse experiment : snapshot.experiments) {
                    if (experiment != null && experiment.experimentId() != null && !experiment.experimentId().isBlank()) {
                        experiments.put(experiment.experimentId(), experiment);
                    }
                }
            }
            killSwitchActive = snapshot.killSwitchActive;
            log.info("Loaded chaos state: killSwitchActive={}, experiments={}", killSwitchActive, experiments.size());
        } catch (IOException e) {
            killSwitchActive = defaultKillSwitchActive;
            experiments.clear();
            log.warn("Failed to load chaos state from {}: {}. Using defaults.",
                    stateFile, e.getMessage());
            persistState();
        }
    }

    private synchronized void persistState() {
        try {
            Path parent = stateFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            ChaosStateSnapshot snapshot = new ChaosStateSnapshot();
            snapshot.killSwitchActive = killSwitchActive;
            snapshot.updatedAt = Instant.now().toString();
            snapshot.experiments = new ArrayList<>(experiments.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), snapshot);
        } catch (IOException e) {
            log.error("Failed to persist chaos state to {}: {}", stateFile, e.getMessage());
        }
    }

    private static class ChaosStateSnapshot {
        public boolean killSwitchActive;
        public String updatedAt;
        public List<ChaosExperimentResponse> experiments = List.of();
    }
}
