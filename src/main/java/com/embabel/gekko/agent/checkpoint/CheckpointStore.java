package com.embabel.gekko.agent.checkpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON file-based checkpoint store for crash recovery.
 * Uses atomic writes (temp file + rename) to prevent corruption.
 * File path: data/checkpoints/<TICKER>.json
 */
@Component
@Slf4j
public class CheckpointStore {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Path checkpointDir;
    private final ObjectMapper mapper;

    public CheckpointStore(
            @Value("${app.checkpoint.dir:data/checkpoints}") String checkpointDir,
            ObjectMapper mapper
    ) {
        this.checkpointDir = Path.of(checkpointDir);
        this.mapper = mapper;
        ensureDirExists();
    }

    private void ensureDirExists() {
        try {
            if (!Files.exists(checkpointDir)) {
                Files.createDirectories(checkpointDir);
            }
        } catch (IOException e) {
            log.warn("Failed to create checkpoint directory {}: {}", checkpointDir, e.getMessage());
        }
    }

    private Path checkpointPath(String ticker) {
        // Sanitize: only allow alphanumeric, dots, hyphens, underscores
        if (!ticker.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid ticker for checkpoint path: " + ticker);
        }
        Path path = checkpointDir.resolve(ticker + ".json").normalize();
        if (!path.startsWith(checkpointDir)) {
            throw new IllegalArgumentException("Path traversal attempt detected for: " + ticker);
        }
        return path;
    }

    /**
     * Save a checkpoint for a given phase with blackboard state.
     */
    public void saveCheckpoint(String ticker, String tradeDate, String phase,
                               Map<String, Object> blackboardState) {
        try {
            Path path = checkpointPath(ticker);
            String content;

            if (Files.exists(path)) {
                // Read existing checkpoint and merge
                String existing = Files.readString(path, StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> existingMap = this.mapper.readValue(existing, Map.class);

                // Update lastCompletedPhase and add/replace the phase
                existingMap.put("lastCompletedPhase", phase);
                @SuppressWarnings("unchecked")
                Map<String, Object> phases = (Map<String, Object>) existingMap.computeIfAbsent(
                        "phases", k -> new HashMap<>());
                phases.put(phase, Map.of("blackboard", blackboardState));
                existingMap.put("savedAt", LocalDateTime.now().format(DTF));

                content = this.mapper.writeValueAsString(existingMap);
            } else {
                // Create new checkpoint
                Map<String, Object> checkpoint = new HashMap<>();
                checkpoint.put("ticker", ticker);
                checkpoint.put("tradeDate", tradeDate);
                checkpoint.put("lastCompletedPhase", phase);
                Map<String, Object> phases = new HashMap<>();
                phases.put(phase, Map.of("blackboard", blackboardState));
                checkpoint.put("phases", phases);
                checkpoint.put("savedAt", LocalDateTime.now().format(DTF));
                content = this.mapper.writeValueAsString(checkpoint);
            }

            atomicWrite(path, content);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize checkpoint for {}: {}", ticker, e.getMessage());
        } catch (IOException e) {
            log.error("Failed to save checkpoint for {}: {}", ticker, e.getMessage());
        }
    }

    /**
     * Get the checkpoint entry for a ticker.
     */
    public CheckpointEntry getCheckpoint(String ticker, String tradeDate) {
        try {
            Path path = checkpointPath(ticker);
            if (!Files.exists(path)) {
                return null;
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(content, Map.class);

            String lastPhase = (String) map.get("lastCompletedPhase");
            @SuppressWarnings("unchecked")
            Map<String, Object> phases = (Map<String, Object>) map.get("phases");
            if (phases == null) phases = Map.of();

            return new CheckpointEntry(
                    (String) map.get("ticker"),
                    (String) map.get("tradeDate"),
                    lastPhase,
                    phases
            );
        } catch (Exception e) {
            log.error("Failed to read checkpoint for {}: {}", ticker, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a checkpoint exists for the ticker.
     */
    public boolean hasCheckpoint(String ticker) {
        return Files.exists(checkpointPath(ticker));
    }

    /**
     * Delete the checkpoint file for a ticker.
     */
    public void deleteCheckpoint(String ticker) {
        try {
            Path path = checkpointPath(ticker);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted checkpoint for {}", ticker);
            }
        } catch (IOException e) {
            log.error("Failed to delete checkpoint for {}: {}", ticker, e.getMessage());
        }
    }

    private void atomicWrite(Path path, String content) throws IOException {
        Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Checkpoint entry returned by getCheckpoint.
     */
    public record CheckpointEntry(
            String ticker,
            String tradeDate,
            String lastCompletedPhase,
            Map<String, Object> phases
    ) {
    }
}
