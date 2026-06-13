package com.embabel.gekko.htmx;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HitlServiceTest {

    @Test
    void createAndGetSession() {
        HitlService service = new HitlService(Duration.ofHours(24));

        HitlService.HitlSession session = service.createSession("proc-1", "TraderAgent", "Test error");

        assertNotNull(session);
        assertEquals("proc-1", session.processId());
        assertEquals("TraderAgent", session.agentName());
        assertEquals("Test error", session.errorMessage());
        assertFalse(session.userActionTaken());
    }

    @Test
    void getSession_returnsEmptyForMissing() {
        HitlService service = new HitlService(Duration.ofHours(24));

        assertTrue(service.getSession("nonexistent").isEmpty());
    }

    @Test
    void updateSession() {
        HitlService service = new HitlService(Duration.ofHours(24));
        service.createSession("proc-1", "TraderAgent", "Test error");

        HitlService.HitlSession updated = service.updateSession("proc-1", "user input", "feedback");

        assertEquals("user input", updated.userInput());
        assertEquals("feedback", updated.feedback());
        assertTrue(updated.userActionTaken());
    }

    @Test
    void updateSession_throwsForMissing() {
        HitlService service = new HitlService(Duration.ofHours(24));

        assertThrows(IllegalArgumentException.class, () ->
                service.updateSession("nonexistent", "input", "feedback"));
    }

    @Test
    void updateSessionWithNewProcessId() {
        HitlService service = new HitlService(Duration.ofHours(24));
        service.createSession("proc-1", "TraderAgent", "Test error");

        HitlService.HitlSession updated = service.updateSession("proc-1", "input", "feedback", "proc-2");

        assertEquals("proc-2", updated.processId());
        assertTrue(updated.userActionTaken());

        // Old processId should no longer have a session
        assertTrue(service.getSession("proc-1").isEmpty());
        // New processId should have the session
        assertEquals("proc-2", service.getSession("proc-2").get().processId());
    }

    @Test
    void removeSession() {
        HitlService service = new HitlService(Duration.ofHours(24));
        service.createSession("proc-1", "TraderAgent", "Test error");

        service.removeSession("proc-1");

        assertTrue(service.getSession("proc-1").isEmpty());
    }

    @Test
    void getActiveSessions_excludesTaken() {
        HitlService service = new HitlService(Duration.ofHours(24));
        service.createSession("proc-1", "TraderAgent", "Error 1");
        service.createSession("proc-2", "TraderAgent", "Error 2");
        service.updateSession("proc-1", "input", "feedback");

        Map<String, HitlService.HitlSession> active = service.getActiveSessions();

        assertEquals(1, active.size());
        assertTrue(active.containsKey("proc-2"));
        assertFalse(active.containsKey("proc-1"));
    }

    @Test
    void createSession_isIdempotent() {
        HitlService service = new HitlService(Duration.ofHours(24));

        HitlService.HitlSession session1 = service.createSession("proc-1", "TraderAgent", "Error 1");
        HitlService.HitlSession session2 = service.createSession("proc-1", "TraderAgent", "Error 2");

        // Second call should return the existing session (not overwrite)
        assertEquals(session1.processId(), session2.processId());
        assertEquals("Error 1", session2.errorMessage()); // Original error preserved
    }
}
