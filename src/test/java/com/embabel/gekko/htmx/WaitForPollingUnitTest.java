package com.embabel.gekko.htmx;

import com.embabel.gekko.util.AgentUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the WAITING state detection infrastructure.
 *
 * Validates:
 * 1. validateProcessId accepts Embabel-style process names (not just UUIDs)
 * 2. validateProcessId rejects empty/null values
 */
class WaitForPollingUnitTest {

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
        assertDoesNotThrow(() -> AgentUtils.validateProcessId("NVDA"),
                "Should accept ticker-like process names");
    }

    @Test
    void validateProcessId_rejectsEmptyAndNull() {
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId(null),
                "Should reject null process ID");
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId(""),
                "Should reject empty process ID");
        assertThrows(ResponseStatusException.class, () -> AgentUtils.validateProcessId("  "),
                "Should reject whitespace-only process ID");
    }

    @Test
    void validateProcessId_rejects_nullWithBadRequest() {
        try {
            AgentUtils.validateProcessId(null);
            fail("Should have thrown ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }
}