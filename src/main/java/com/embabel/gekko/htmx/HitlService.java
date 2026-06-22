package com.embabel.gekko.htmx;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;

/**
 * Human-in-the-Loop service that stores error state for agent processes
 * and allows humans to provide input/feedback to retry failed processes.
 */
public class HitlService implements DisposableBean {

    /**
     * Represents a human-in-the-loop session for a failed agent process.
     */
    public record HitlSession(
            String processId,
            String agentName,
            String errorMessage,
            LocalDateTime occurredAt,
            String userInput,
            String feedback,
            boolean userActionTaken
    ) {
        public HitlSession {
            if (userInput == null) userInput = "";
            if (feedback == null) feedback = "";
        }
    }

    private final Map<String, HitlSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Duration sessionTtl;

    /**
     * Create a new HitlService with the given TTL for session cleanup.
     * Sessions older than TTL will be automatically removed.
     */
    public HitlService(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
        // Schedule periodic cleanup every 5 minutes
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Create a new HitlService with a default 24-hour TTL.
     */
    public HitlService() {
        this(Duration.ofHours(24));
    }

    /**
     * Create a HITL session for a failed process.
     * Uses computeIfAbsent for true atomic check-and-create — prevents duplicate sessions
     * from concurrent event firings.
     */
    public HitlSession createSession(String processId, String agentName, String errorMessage) {
        return sessions.computeIfAbsent(processId, id -> new HitlSession(
                id,
                agentName,
                errorMessage,
                LocalDateTime.now(),
                "",
                "",
                false
        ));
    }

    /**
     * Get the HITL session for a process, if any.
     */
    public Optional<HitlSession> getSession(String processId) {
        return Optional.ofNullable(sessions.get(processId));
    }

    /**
     * Update a session with user input and feedback.
     * Uses compute() for atomic read-modify-write — prevents concurrent updates from silently overwriting.
     */
    public HitlSession updateSession(String processId, String userInput, String feedback) {
        return sessions.compute(processId, (key, existing) -> {
            if (existing == null) {
                throw new IllegalArgumentException("No HITL session found for process: " + processId);
            }
            return new HitlSession(
                    processId,
                    existing.agentName(),
                    existing.errorMessage(),
                    existing.occurredAt(),
                    userInput,
                    feedback,
                    true
            );
        });
    }

    /**
     * Update a session with user input, feedback, and the new process ID.
     * Per-session locking to avoid global serialization.
     */
    public HitlSession updateSession(String processId, String userInput, String feedback, String newProcessId) {
        Object lock = sessionLocks.computeIfAbsent(processId, k -> new Object());
        synchronized (lock) {
            HitlSession session = sessions.get(processId);
            if (session == null) {
                throw new IllegalArgumentException("No HITL session found for process: " + processId);
            }
            HitlSession updated = new HitlSession(
                    newProcessId,
                    session.agentName(),
                    session.errorMessage(),
                    session.occurredAt(),
                    userInput,
                    feedback,
                    true
            );
            sessions.remove(processId);
            sessions.put(newProcessId, updated);
            sessionLocks.remove(processId, lock);
            return updated;
        }
    }

    /**
     * Remove a session (e.g., after retry is complete).
     */
    public void removeSession(String processId) {
        sessions.remove(processId);
    }

    /**
     * Get all active (unresolved) sessions.
     */
    public Map<String, HitlSession> getActiveSessions() {
        Map<String, HitlSession> result = new HashMap<>();
        sessions.forEach((k, v) -> {
            if (!v.userActionTaken()) {
                result.put(k, v);
            }
        });
        return result;
    }

    /**
     * Clean up expired sessions (older than TTL).
     */
    private void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(sessionTtl);
        sessions.entrySet().removeIf(e -> e.getValue().occurredAt().isBefore(cutoff));
    }

    /**
     * Shut down the cleanup scheduler when the Spring context closes.
     */
    @Override
    public void destroy() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
