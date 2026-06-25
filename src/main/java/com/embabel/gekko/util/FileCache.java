package com.embabel.gekko.util;

import com.embabel.gekko.domain.ResearchTypes;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
public class FileCache {

    private final File baseDir;
    private final ObjectMapper mapper;
    /** Per-key locks to prevent concurrent duplicate computation. */
    private final Map<String, Object> lockMap = new ConcurrentHashMap<>();

    public FileCache() {
        this.baseDir = new File("data/llm/cache");
        if (!baseDir.exists()) baseDir.mkdirs();

        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Sanitizes a cache key to prevent path traversal attacks.
     * Only strips path traversal sequences — the SHA-256 hash handles uniqueness.
     */
    private String sanitizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key must not be null or blank");
        }
        // Strip only path traversal sequences
        String sanitized = key
                .replace("..", "")
                .replace("/", "")
                .replace("\\", "")
                .replace("\0", "");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Cache key must not be empty after sanitization");
        }
        return sanitized;
    }

    private String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(key.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private File fileForKey(String key, String extension) {
        String sanitized = sanitizeKey(key);
        String hashed = hashKey(sanitized);
        return new File(baseDir, hashed + extension);
    }

    /**
     * Get a cached value by key, or return null if not found.
     */
    public <T> T get(String key, Class<T> clazz) {
        File jsonFile = fileForKey(key, ".json");
        File mdFile = fileForKey(key, ".md");

        try {
            if (jsonFile.exists()) {
                return mapper.readValue(jsonFile, clazz);
            } else if (mdFile.exists()) {
                String content = Files.readString(mdFile.toPath(), StandardCharsets.UTF_8);
                return clazz.cast(content);
            }
        } catch (IOException ex) {
            log.error("Failed to read cache for key {}: {}", key, ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * Get a cached value by key, or compute and save it using the supplier.
     * Uses per-key locking to prevent concurrent duplicate computation.
     * Both threads requesting the same key will compute exactly once.
     * Different keys compute independently.
     */
    public <T> T getOrCompute(String key, Class<T> clazz, Supplier<T> supplier) {
        // Fast path: check cache without locking
        T cached = get(key, clazz);
        if (cached != null) {
            return cached;
        }

        // Per-key locking: get or create a lock object for this key
        Object lock = lockMap.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            // Double-check after acquiring lock (another thread may have computed)
            cached = get(key, clazz);
            if (cached != null) {
                return cached;
            }

            try {
                T value = supplier.get();
                save(key, value);
                return value;
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException) {
                    log.warn("Failed to save cache for key {} after successful computation: {}", key, e.getMessage());
                }
                throw e;
            } finally {
                // Clean up the per-key lock after successful computation
                // to allow new keys to be computed in the future
                lockMap.remove(key, lock);
            }
        }
    }

    /**
     * Save a value to the cache. Uses atomic write (temp file + rename)
     * to prevent partial read corruption.
     */
    public void save(String key, Object value) {
        try {
            if (value instanceof ResearchTypes.Report report) {
                mapper.writeValue(fileForKey(key, ".json"), value);
                saveMarkdown(key, report.content());
            } else if (value instanceof String string) {
                saveMarkdown(key, string);
            } else {
                mapper.writeValue(fileForKey(key, ".json"), value);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save cache for key " + key + ": " + e.getMessage(), e);
        }
    }

    private void saveMarkdown(String key, String markdown) throws RuntimeException {
        File mdFile = fileForKey(key, ".md");
        File tempFile = new File(mdFile.getParentFile(), mdFile.getName() + ".tmp");
        try (FileWriter fw = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
            fw.write(markdown);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write cache for key " + key + ": " + e.getMessage(), e);
        }
        // Atomic rename: temp file → final file
        try {
            Files.move(tempFile.toPath(), mdFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to atomically save cache for key {}: {}", key, e.getMessage(), e);
        }
    }
}
