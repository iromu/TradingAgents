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

    private File fileForKey(String key) {
//        return new File(baseDir, hashKey(key) + ".json");
        return new File(baseDir, key.toUpperCase() + ".json");
    }

    private File fileForMarkdown(String key) {
//        return new File(baseDir, hashKey(key) + ".md");
        return new File(baseDir, key.toUpperCase() + ".md");
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        File f = fileForKey(key);
        if (!f.exists()) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(f, clazz));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return Optional.empty();
        }
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
            // Also write Markdown if the object is a Report
            if (value instanceof TraderAgent.Report report) {
                mapper.writeValue(fileForKey(key), value);
                saveMarkdown(key, report.content());
            } else if (value instanceof String string) {
                saveMarkdown(key, string);
            } else
                mapper.writeValue(fileForKey(key), value);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void saveMarkdown(String key, String markdown) throws IOException {
        File mdFile = fileForMarkdown(key);

        try (FileWriter fw = new FileWriter(mdFile, StandardCharsets.UTF_8)) {
            fw.write(markdown);
        }
    }

}
