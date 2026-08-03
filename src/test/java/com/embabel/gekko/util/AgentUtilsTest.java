package com.embabel.gekko.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AgentUtilsTest {

    @Test
    void validateProcessId_acceptsValidIds() {
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("pedantic_elgamal"));
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("agent-1"));
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("Agent123"));
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("a"));
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("my_agent_123-test"));
    }

    @Test
    void validateProcessId_rejectsNull() {
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId(null));
    }

    @Test
    void validateProcessId_rejectsBlank() {
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId(""));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("   "));
    }

    @Test
    void validateProcessId_rejectsInvalidCharacters() {
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent name"));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent.name"));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent/name"));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent<name>"));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent;name"));
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("agent`name"));
    }
}
