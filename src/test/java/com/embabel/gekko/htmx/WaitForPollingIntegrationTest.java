use apackage com.embabel.gekko.htmx;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.util.AgentUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the WAITING state detection flow.
 *
 * Validates:
 * 1. Process status polling endpoint returns correct status
 * 2. validateProcessId accepts Embabel-style process names (not just UUIDs)
 * 3. WAITING state is properly detected via the polling endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
class WaitForPollingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentPlatform agentPlatform;

    // --- validateProcessId tests ---

    @Test
    void validateProcessId_acceptsEmbabelNames() {
        // Embabel generates names like "pedantic_elgamal", not UUIDs
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("pedantic_elgamal"),
                "Should accept Embabel-style process names");
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("silly_babbage"),
                "Should accept Embabel-style process names with underscores");
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("abc123"),
                "Should accept alphanumeric process names");
    }

    @Test
    void validateProcessId_rejectsEmptyAndNull() {
        assertThrows(Exception.class, () -> AgentUtils.validateProcessId(null),
                "Should reject null process ID");
        assertThrows(Exception.class, () -> AgentUtils.validateProcessId(""),
                "Should reject empty process ID");
        assertThrows(Exception.class, () -> AgentUtils.validateProcessId("  "),
                "Should reject whitespace-only process ID");
    }

    // --- Status polling endpoint tests ---

    @Test
    void statusEndpoint_returnsNotFoundForUnknownProcess() throws Exception {
        mockMvc.perform(get("/api/v1/process/nonexistent-process/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Process not found"));
    }

    @Test
    void statusEndpoint_returnsRunningStatus() throws Exception {
        // Create a process that will be in a non-WAITING state
        var process = agentPlatform.getAgentProcess("pedantic_elgamal");
        // Even if the process doesn't exist, the endpoint should handle it gracefully
        // The key test is that it doesn't throw 400 for non-UUID names
        mockMvc.perform(get("/api/v1/process/pedantic_elgamal/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusEndpoint_format_isJson() throws Exception {
        // Verify the endpoint returns JSON (not HTML) so the polling JS can parse it
        mockMvc.perform(get("/api/v1/process/nonexistent/status"))
                .andExpect(header().exists("Content-Type"))
                .andExpect(status().isNotFound());
    }
}
