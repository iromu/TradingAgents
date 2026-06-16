package com.embabel.gekko.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * File-based repository for decision memory log.
 * Uses atomic writes (temp file + rename) and regex parsing matching Python's format.
 */
@Component
@Slf4j
public class DecisionMemoryRepository {

    private static final String ENTRY_SEPARATOR = "<!-- ENTRY_END -->";
    // Matches: [2026-01-15 | NVDA | Buy | pending]
    private static final Pattern PENDING_ENTRY_RE = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2})\\s*\\|\\s*(\\S+)\\s*\\|\\s*(\\S+)\\s*\\|\\s*(pending)\\]",
            Pattern.CASE_INSENSITIVE
    );
    // Matches: [2026-01-15 | AAPL | Hold | +3.2% | +1.5% | 5d]
    private static final Pattern RESOLVED_ENTRY_RE = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2})\\s*\\|\\s*(\\S+)\\s*\\|\\s*(\\S+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*(\\d+)d\\]",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DECISION_BLOCK_RE = Pattern.compile(
            "DECISION:\\s*\\n\\*\\*Rating\\*\\*:\\s*(.+?)\\s*\\n\\n\\*\\*Executive Summary\\*\\*:\\s*(.+?)\\s*\\n\\n\\*\\*Investment Thesis\\*\\*:\\s*(.+?)(?=\\n\\n<!--|\\Z)",
            Pattern.DOTALL
    );
    private static final Pattern REFLECTION_BLOCK_RE = Pattern.compile(
            "REFLECTION:\\s*\\n(.+?)(?=\\n\\n<!--|\\Z)",
            Pattern.DOTALL
    );
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path memoryLogPath;
    private final int maxEntries;
    private final ObjectMapper mapper;

    public DecisionMemoryRepository(
            @Value("${app.memory.log-path:~/.tradingagents/memory/trading_memory.md}") String logPath,
            @Value("${app.memory.log-max-entries:0}") int maxEntries
    ) {
        this.memoryLogPath = Path.of(logPath.replace("~", System.getProperty("user.home")));
        this.maxEntries = maxEntries;
        this.mapper = new ObjectMapper();
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            Path parent = memoryLogPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(memoryLogPath)) {
                Files.createFile(memoryLogPath);
            }
        } catch (IOException e) {
            log.warn("Failed to ensure memory log file exists at {}: {}", memoryLogPath, e.getMessage());
        }
    }

    /**
     * Append a pending decision entry atomically.
     */
    public void appendPending(String ticker, String tradeDate, String rating,
                              String executiveSummary, String investmentThesis) {
        String entry = buildPendingEntry(ticker, tradeDate, rating, executiveSummary, investmentThesis);
        atomicAppend(entry);
    }

    private String buildPendingEntry(String ticker, String tradeDate, String rating,
                                     String executiveSummary, String investmentThesis) {
        return """
                [%s | %s | %s | pending]

                DECISION:
                **Rating**: %s

                **Executive Summary**: %s

                **Investment Thesis**: %s

                %s
                """.formatted(
                tradeDate, ticker, rating,
                rating, executiveSummary, investmentThesis,
                ENTRY_SEPARATOR
        );
    }

    /**
     * Resolve a pending decision with actual returns and reflection.
     * Atomically updates the log file.
     */
    public void resolve(String ticker, String tradeDate, BigDecimal rawReturn,
                        BigDecimal alphaReturn, String benchmark, int daysHeld,
                        String reflection) {
        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            String[] entries = splitEntries(content);
            StringBuilder newContent = new StringBuilder();
            boolean resolved = false;

            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;

                Matcher matcher = PENDING_ENTRY_RE.matcher(entry);
                if (matcher.find() && matcher.group(2).equals(ticker) && matcher.group(1).equals(tradeDate)) {
                    String resolvedEntry = buildResolvedEntry(ticker, tradeDate, matcher.group(3),
                            rawReturn, alphaReturn, daysHeld, reflection);
                    newContent.append(resolvedEntry).append("\n\n");
                    resolved = true;
                } else {
                    newContent.append(entry).append("\n\n");
                }
            }

            if (!resolved) {
                log.warn("No pending entry found for {} on {}", ticker, tradeDate);
                return;
            }

            atomicWrite(newContent.toString().trim());
        } catch (IOException e) {
            log.error("Failed to resolve decision for {} on {}: {}", ticker, tradeDate, e.getMessage());
        }
    }

    private String buildResolvedEntry(String ticker, String tradeDate, String rating,
                                      BigDecimal rawReturn, BigDecimal alphaReturn,
                                      int daysHeld, String reflection) {
        return """
                [%s | %s | %s | %s | %s | %dd]

                DECISION:
                **Rating**: %s

                **Executive Summary**: N/A

                REFLECTION:
                %s

                %s
                """.formatted(
                tradeDate, ticker, rating,
                rawReturn, alphaReturn, daysHeld,
                rating, reflection,
                ENTRY_SEPARATOR
        );
    }

    /**
     * Check if there are pending entries for the given ticker.
     */
    public boolean hasPendingEntriesFor(String ticker) {
        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            Matcher matcher = PENDING_ENTRY_RE.matcher(content);
            while (matcher.find()) {
                if (matcher.group(2).equals(ticker)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to check pending entries for {}: {}", ticker, e.getMessage());
            return false;
        }
    }

    /**
     * Get pending entries for a specific ticker.
     */
    public List<PendingDecision> getPendingEntries(String ticker) {
        List<PendingDecision> results = new ArrayList<>();
        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            String[] entries = splitEntries(content);

            for (String entry : entries) {
                Matcher headerMatcher = PENDING_ENTRY_RE.matcher(entry);
                if (headerMatcher.find() && headerMatcher.group(2).equals(ticker)) {
                    Matcher decisionMatcher = DECISION_BLOCK_RE.matcher(entry);
                    String rating = headerMatcher.group(3);
                    String executiveSummary = "N/A";
                    String investmentThesis = "N/A";

                    if (decisionMatcher.find()) {
                        rating = decisionMatcher.group(1).trim();
                        executiveSummary = decisionMatcher.group(2).trim();
                        investmentThesis = decisionMatcher.group(3).trim();
                    }

                    results.add(new PendingDecision(
                            headerMatcher.group(2),
                            headerMatcher.group(1),
                            rating,
                            executiveSummary,
                            investmentThesis,
                            LocalDateTime.now()
                    ));
                }
            }
        } catch (IOException e) {
            log.error("Failed to get pending entries for {}: {}", ticker, e.getMessage());
        }
        return results;
    }

    /**
     * Generate past_context string: 5 same-ticker + 3 cross-ticker lessons.
     */
    public String generatePastContext(String ticker) {
        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            String[] entries = splitEntries(content);

            List<String> sameTicker = new ArrayList<>();
            List<String> crossTicker = new ArrayList<>();

            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;

                Matcher headerMatcher = PENDING_ENTRY_RE.matcher(entry);
                if (headerMatcher.find()) continue; // Skip pending entries

                Matcher resolvedMatcher = RESOLVED_ENTRY_RE.matcher(entry);
                if (!resolvedMatcher.find()) continue;

                String entryTicker = resolvedMatcher.group(2);
                Matcher reflectionMatcher = REFLECTION_BLOCK_RE.matcher(entry);

                String reflection = "N/A";
                if (reflectionMatcher.find()) {
                    reflection = reflectionMatcher.group(1).trim();
                }

                String lesson = String.format("[%s] %s: %s", entryTicker, resolvedMatcher.group(3), reflection);

                if (entryTicker.equals(ticker)) {
                    sameTicker.add(lesson);
                } else {
                    crossTicker.add(lesson);
                }
            }

            // Take up to 5 same-ticker and 3 cross-ticker (most recent = last in list)
            int sameStart = Math.max(0, sameTicker.size() - 5);
            int crossStart = Math.max(0, crossTicker.size() - 3);

            List<String> context = new ArrayList<>();
            context.addAll(sameTicker.subList(sameStart, sameTicker.size()));
            context.addAll(crossTicker.subList(crossStart, crossTicker.size()));

            if (context.isEmpty()) {
                return "";
            }

            return "PAST DECISION MEMORY:\n" + String.join("\n---\n", context);

        } catch (IOException e) {
            log.error("Failed to generate past context for {}: {}", ticker, e.getMessage());
            return "";
        }
    }

    /**
     * Rotate (prune oldest resolved entries) if over max-entries limit.
     */
    public void rotate() {
        if (maxEntries <= 0) return;

        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            String[] entries = splitEntries(content);

            // Count resolved entries (non-pending)
            int resolvedCount = 0;
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                Matcher pendingMatcher = PENDING_ENTRY_RE.matcher(entry);
                if (!pendingMatcher.find()) {
                    resolvedCount++;
                }
            }

            if (resolvedCount <= maxEntries) return;

            // Keep pending entries, prune oldest resolved
            List<String> pendingEntries = new ArrayList<>();
            List<String> resolvedEntries = new ArrayList<>();

            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                Matcher pendingMatcher = PENDING_ENTRY_RE.matcher(entry);
                if (pendingMatcher.find()) {
                    pendingEntries.add(entry);
                } else {
                    resolvedEntries.add(entry);
                }
            }

            // Prune oldest resolved entries
            int toKeep = Math.min(maxEntries, resolvedEntries.size());
            int toRemove = resolvedEntries.size() - toKeep;
            resolvedEntries = resolvedEntries.subList(toRemove, resolvedEntries.size());

            StringBuilder newContent = new StringBuilder();
            for (String entry : pendingEntries) {
                newContent.append(entry).append("\n\n");
            }
            for (String entry : resolvedEntries) {
                newContent.append(entry).append("\n\n");
            }

            atomicWrite(newContent.toString().trim());
            log.info("Rotated memory log: removed {} oldest resolved entries", toRemove);

        } catch (IOException e) {
            log.error("Failed to rotate memory log: {}", e.getMessage());
        }
    }

    /**
     * Recover from corruption by truncating to last complete entry.
     */
    public void recoverFromCorruption() {
        try {
            String content = Files.readString(memoryLogPath, StandardCharsets.UTF_8);
            int lastSeparator = content.lastIndexOf(ENTRY_SEPARATOR);
            if (lastSeparator > 0) {
                String recovered = content.substring(0, lastSeparator + ENTRY_SEPARATOR.length());
                atomicWrite(recovered);
                log.info("Recovered memory log from corruption, truncated to last complete entry");
            }
        } catch (IOException e) {
            log.error("Failed to recover memory log from corruption: {}", e.getMessage());
        }
    }

    // --- Split entries helper ---

    /**
     * Split memory log content into individual entries by ENTRY_SEPARATOR.
     * Handles the separator as a delimiter (not a lookahead), preserving it with each entry.
     */
    private String[] splitEntries(String content) {
        if (content == null || content.isBlank()) return new String[0];
        // Split on the separator, keeping it with each entry
        String[] raw = content.split(Pattern.quote(ENTRY_SEPARATOR));
        // Reattach separator to each entry
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < raw.length; i++) {
            String entry = raw[i].trim();
            if (entry.isEmpty()) continue;
            if (i < raw.length - 1) {
                entry = entry + "\n" + ENTRY_SEPARATOR;
            }
            entries.add(entry);
        }
        return entries.toArray(new String[0]);
    }

    // --- Atomic write helpers ---

    private void atomicAppend(String entry) {
        try {
            String existing = Files.exists(memoryLogPath)
                    ? Files.readString(memoryLogPath, StandardCharsets.UTF_8).trim()
                    : "";
            String combined = existing.isEmpty() ? entry : existing + "\n\n" + entry;
            atomicWrite(combined + "\n");
        } catch (IOException e) {
            log.error("Failed to append to memory log: {}", e.getMessage());
        }
    }

    private void atomicWrite(String content) {
        try {
            Path tempFile = memoryLogPath.resolveSibling(memoryLogPath.getFileName() + ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            Files.move(tempFile, memoryLogPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write memory log: {}", e.getMessage());
        }
    }
}
