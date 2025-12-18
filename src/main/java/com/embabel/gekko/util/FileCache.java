package com.embabel.gekko.util;

import com.embabel.gekko.agent.TraderAgent;
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
import java.security.MessageDigest;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
public class FileCache {

    private final File baseDir;
    private final ObjectMapper mapper;

    public FileCache() {
        this.baseDir = new File("data/llm/cache");
        if (!baseDir.exists()) baseDir.mkdirs();

        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(key.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File fileForKey(String key, String extension) {
        return new File(baseDir, key.toUpperCase() + extension);
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        File jsonFile = fileForKey(key, ".json");
        File mdFile = fileForKey(key, ".md");

        try {
            if (jsonFile.exists()) {
                return Optional.of(mapper.readValue(jsonFile, clazz));
            } else if (mdFile.exists()) {
                String content = Files.readString(mdFile.toPath(), StandardCharsets.UTF_8);
                return Optional.of(clazz.cast(content));
            }
        } catch (IOException ex) {
            log.error("Failed to read cache for key {}: {}", key, ex.getMessage(), ex);
        }

        return Optional.empty();
    }

    public <T> T getOrCompute(String key, Class<T> clazz, Supplier<T> supplier) {
        return get(key, clazz).orElseGet(() -> {
            T value = supplier.get();
            save(key, value);
            return value;
        });
    }

    public void save(String key, Object value) {
        try {
            if (value instanceof TraderAgent.Report report) {
                mapper.writeValue(fileForKey(key, ".json"), value);
                saveMarkdown(key, report.content());
            } else if (value instanceof String string) {
                saveMarkdown(key, string);
            } else {
                mapper.writeValue(fileForKey(key, ".json"), value);
            }
        } catch (IOException e) {
            log.error("Failed to save cache for key {}: {}", key, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void saveMarkdown(String key, String markdown) throws IOException {
        File mdFile = fileForKey(key, ".md");
        try (FileWriter fw = new FileWriter(mdFile, StandardCharsets.UTF_8)) {
            fw.write(markdown);
        }
    }
}
