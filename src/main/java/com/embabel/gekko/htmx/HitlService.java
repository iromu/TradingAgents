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
 * In-memory HITL session store with TTL-based cleanup.
 * Sessions are lost on restart — acceptable for short-lived workflows.
 */
public class HitlService implements DisposableBean {

    /**
     * Represents a human-in-the-loop session for a failed agent process.
     * Immutable snapshot of session state at time of creation.
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
    private final ScheduledExecutorService cleanupScheduler;
    private final Duration sessionTtl;
    private final int maxSessions;

    /** Maximum number of sessions allowed before eviction. */
    public static final int DEFAULT_MAX_SESSIONS = 1000;

    /**
     * Create a new HitlService with the given TTL for session cleanup.
     * Sessions older than TTL will be automatically removed.
     *
     * @deprecated Use {@link #HitlService(Duration, ScheduledExecutorService)} with an injected scheduler.
     */
    @Deprecated
    public HitlService(Duration sessionTtl) {
        this(sessionTtl, DEFAULT_MAX_SESSIONS, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));
    }

    /**
     * Create a new HitlService with custom TTL and max session cap.
     *
     * @deprecated Use {@link #HitlService(Duration, ScheduledExecutorService)} with an injected scheduler.
     */
    @Deprecated
    public HitlService(Duration sessionTtl, int maxSessions) {
        this(sessionTtl, maxSessions, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));
    }

    /**
     * Create a new HitlService with an injected cleanup scheduler.
     */
    public HitlService(Duration sessionTtl, ScheduledExecutorService cleanupScheduler) {
        this(sessionTtl, DEFAULT_MAX_SESSIONS, cleanupScheduler);
    }

    /**
     * Create a new HitlService with custom TTL, max sessions, and injected scheduler.
     */
    public HitlService(Duration sessionTtl, int maxSessions, ScheduledExecutorService cleanupScheduler) {
        this.sessionTtl = sessionTtl;
        this.maxSessions = maxSessions;
        this.cleanupScheduler = cleanupScheduler;
        // Schedule periodic cleanup every 5 minutes
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Create a HITL session for a failed process.
     * Atomic via computeIfAbsent — safe for concurrent event firings.
     */
    public HitlSession createSession(String processId, String agentName, String errorMessage) {
        evictIfFull();
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
     * Atomic via compute() — prevents concurrent update conflicts.
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
     * Evict sessions if max capacity is reached.
     * Prefers expired sessions (past TTL); if none expired, evicts the LRU session.
     */
    private void evictIfFull() {
        if (sessions.size() >= maxSessions) {
            LocalDateTime cutoff = LocalDateTime.now().minus(sessionTtl);
            String oldestExpiredKey = sessions.entrySet().stream()
                    .filter(e -> e.getValue().occurredAt().isBefore(cutoff))
                    .min(Map.Entry.<String, HitlSession>comparingByValue(
                            java.util.Comparator.comparing(HitlService.HitlSession::occurredAt)
                    ))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestExpiredKey != null) {
                sessions.remove(oldestExpiredKey);
                return;
            }
            // No expired sessions — evict LRU (oldest) session
            String lruKey = sessions.entrySet().stream()
                    .min(Map.Entry.<String, HitlSession>comparingByValue(
                            java.util.Comparator.comparing(HitlService.HitlSession::occurredAt)
                    ))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (lruKey != null) {
                sessions.remove(lruKey);
            }
        }
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
