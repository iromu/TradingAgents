package com.embabel.gekko.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Single result-caching contract for the trading pipeline.
 * Backed by the existing disk {@link FileCache}.
 *
 * <ul>
 *   <li>Keys are case-normalized (symbol inputs uppercased) and must cover all
 *       result-affecting inputs.</li>
 *   <li>Error/rate-limit/empty payloads are never persisted.</li>
 *   <li>Time-sensitive categories carry a configurable TTL; stale entries are
 *       treated as misses.</li>
 * </ul>
 */
@Slf4j
@Component
public class ResultCache {

    public static final String CATEGORY_LLM = "llm";
    public static final String CATEGORY_QUOTE = "quote";
    public static final String CATEGORY_EXTERNAL_HTTP = "external_http";

    private final FileCache fileCache;
    private final Map<String, Duration> categoryTtls;
    private final Map<String, Instant> writeTimestamps = new ConcurrentHashMap<>();

    public ResultCache(
            FileCache fileCache,
            @Value("${app.cache.ttl.quote:5m}") String quoteTtl,
            @Value("${app.cache.ttl.external-http:1h}") String externalHttpTtl
    ) {
        this.fileCache = fileCache;
        this.categoryTtls = Map.of(
                CATEGORY_QUOTE, parseDuration(quoteTtl),
                CATEGORY_EXTERNAL_HTTP, parseDuration(externalHttpTtl)
        );
    }

    /**
     * Get a cached value by canonical key, or compute and save it.
     * Applies TTL check for time-sensitive categories.
     * Does NOT delegate to FileCache.getOrCompute — uses get + save directly
     * so that TTL expiry forces a genuine recomputation.
     */
    public <T> T getOrCompute(String category, String canonicalKey, Class<T> clazz, Supplier<T> supplier) {
        T cached = getWithTtl(category, canonicalKey, clazz);
        if (cached != null) {
            return cached;
        }
        T value = supplier.get();
        if (!isErrorPayload(value)) {
            fileCache.save(canonicalKey, value);
            writeTimestamps.put(canonicalKey, Instant.now());
        } else {
            log.debug("Not caching error/empty payload for key {}", canonicalKey);
        }
        return value;
    }

    /**
     * Read a cached value, respecting the category TTL.
     * Returns null on miss or expiry.
     */
    public <T> T get(String category, String canonicalKey, Class<T> clazz) {
        return getWithTtl(category, canonicalKey, clazz);
    }

    /**
     * Explicitly save a value after validating it is not an error payload.
     */
    public void save(String category, String canonicalKey, Object value) {
        if (isErrorPayload(value)) {
            log.debug("Refusing to cache error/empty payload for key {}", canonicalKey);
            return;
        }
        fileCache.save(canonicalKey, value);
        writeTimestamps.put(canonicalKey, Instant.now());
    }

    /**
     * Build a canonical cache key from a category namespace and input parts.
     * All parts are uppercased to ensure case-insensitive matching; parts are
     * joined with a separator that cannot appear in ticker symbols.
     */
    public static String canonicalKey(String category, String... parts) {
        StringBuilder sb = new StringBuilder(category.toLowerCase());
        for (String rawPart : parts) {
            String part = (rawPart != null ? rawPart : "").toUpperCase();
            sb.append('\u001F').append(part);
        }
        return sb.toString();
    }

    /**
     * Normalize a symbol/ticker to uppercase for use in cache keys.
     */
    public static String normalizeSymbol(String symbol) {
        return symbol != null ? symbol.toUpperCase() : "";
    }

    private <T> T getWithTtl(String category, String canonicalKey, Class<T> clazz) {
        T cached = fileCache.get(canonicalKey, clazz);
        if (cached == null) {
            return null;
        }
        Duration ttl = categoryTtls.get(category);
        if (ttl == null) {
            return cached;
        }
        Instant writtenAt = writeTimestamps.get(canonicalKey);
        if (writtenAt == null) {
            // No in-memory timestamp (e.g. fresh JVM): treat as expired so we refetch.
            log.debug("No timestamp for key {} (category {}) — treating as miss", canonicalKey, category);
            return null;
        }
        if (Duration.between(writtenAt, Instant.now()).compareTo(ttl) > 0) {
            log.debug("Cache entry expired for key {} (category {})", canonicalKey, category);
            return null;
        }
        return cached;
    }

    /**
     * Detect error/rate-limit/empty payloads that must never be cached.
     */
    private boolean isErrorPayload(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            if (s.isBlank()) {
                return true;
            }
            String lower = s.toLowerCase();
            return lower.contains("rate limit") || lower.contains("rate_limit")
                    || lower.contains("error") && (lower.contains("invalid") || lower.contains("not found")
                    || lower.contains("exceeded") || lower.contains("too many requests"))
                    || lower.startsWith("{") && lower.contains("\"error\"");
        }
        return false;
    }

    private static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ZERO;
        }
        String v = value.trim().toLowerCase();
        if (v.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(v.substring(0, v.length() - 2)));
        }
        if (v.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        if (v.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        if (v.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        if (v.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        return Duration.ofMinutes(Long.parseLong(v));
    }
}
