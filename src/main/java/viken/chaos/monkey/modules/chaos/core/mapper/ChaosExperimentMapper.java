package viken.chaos.monkey.modules.chaos.core.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import viken.chaos.monkey.common.dto.ChaosExperimentRequest;
import viken.chaos.monkey.common.dto.ChaosExperimentResponse;
import viken.chaos.monkey.modules.chaos.core.entity.ChaosExperimentEntity;

import java.util.Collections;
import java.util.Map;

@Component
public class ChaosExperimentMapper {

    private final ObjectMapper objectMapper;

    public ChaosExperimentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChaosExperimentResponse toResponse(ChaosExperimentEntity entity) {
        return ChaosExperimentResponse.of(
                entity.getUid(),
                entity.getType(),
                entity.getTargetService(),
                entity.getStatus(),
                entity.getResultMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    public Map<String, String> readParams(ChaosExperimentEntity entity) {
        try {
            return objectMapper.readValue(entity.getParamsJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    public String writeParams(ChaosExperimentRequest request) {
        try {
            Map<String, String> params = request.params() != null ? request.params() : Collections.emptyMap();
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
