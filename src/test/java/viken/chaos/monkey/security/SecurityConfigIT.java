package viken.chaos.monkey.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "chaos.state.file=${java.io.tmpdir}/chaos-state-security-${random.uuid}.json",
        "chaos.security.viewer.password=test-viewer",
        "chaos.security.operator.password=test-operator",
        "chaos.security.admin.password=test-admin",
        "chaos.security.auditor.password=test-auditor"
})
class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createExperimentWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/chaos/experiments")
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotCreateExperiment() throws Exception {
        mockMvc.perform(post("/api/chaos/experiments")
                        .with(httpBasic("chaos-viewer", "test-viewer"))
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanCreateExperiment() throws Exception {
        mockMvc.perform(post("/api/chaos/experiments")
                        .with(httpBasic("chaos-operator", "test-operator"))
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void operatorCannotToggleKillSwitch() throws Exception {
        mockMvc.perform(post("/api/chaos/emergency/disable")
                        .with(httpBasic("chaos-operator", "test-operator")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanToggleKillSwitch() throws Exception {
        mockMvc.perform(post("/api/chaos/emergency/disable")
                        .with(httpBasic("chaos-admin", "test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.killSwitchActive").value(true));
    }

    private String validRequestBody() {
        return """
                {
                  "type":"pod-kill",
                  "targetService":"order-service",
                  "namespace":"default",
                  "params":{"blastRadius":"1"}
                }
                """;
    }
}
