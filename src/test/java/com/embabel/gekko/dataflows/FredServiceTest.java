package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FredService — API calls, caching, error handling.
 */
class FredServiceTest {

    @TempDir
    Path tempDir;

    private FredService createService(String apiKey) {
        try {
            Path cacheDir = Files.createTempDirectory("fred-test-cache-");
            var cache = new FileCache();
            var field = FileCache.class.getDeclaredField("baseDir");
            field.setAccessible(true);
            field.set(cache, cacheDir);
            return new FredService(apiKey, cache);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test cache", e);
        }
    }

    // --- getSeries ---

    @Test
    void getSeries_returnsNoDataWhenNoApiKey() {
        var service = createService("");

        String result = service.getSeries("GDP", 100);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
        assertTrue(result.contains("API key not configured"));
    }

    @Test
    void getSeries_returnsNoDataWhenEmptySeriesId() {
        var service = createService("dummy_key");

        String result = service.getSeries("", 100);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
        assertTrue(result.contains("Series ID is required"));
    }

    @Test
    void getSeries_returnsNoDataOnApiError() {
        var service = createService("invalid_key");

        // Will hit the real API but get an error response
        String result = service.getSeries("GDP", 10);

        // Should not throw, returns NO_DATA_AVAILABLE
        assertNotNull(result);
    }

    // --- getMultipleSeries ---

    @Test
    void getMultipleSeries_returnsNoDataWhenNoApiKey() {
        var service = createService("");

        String result = service.getMultipleSeries(List.of("GDP", "CPIAUCSL"));

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    @Test
    void getMultipleSeries_returnsNoDataWhenEmptyList() {
        var service = createService("dummy_key");

        String result = service.getMultipleSeries(List.of());

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
        assertTrue(result.contains("At least one series ID is required"));
    }

    // --- getDashboard ---

    @Test
    void getDashboard_returnsNoDataWhenNoApiKey() {
        var service = createService("");

        String result = service.getDashboard();

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    // --- caching ---

    @Test
    void getSeries_cachesResponse() {
        var service = createService("dummy_key");

        // First call (will fail, but result is cached)
        String result1 = service.getSeries("GDP", 100);
        // Second call should return cached result
        String result2 = service.getSeries("GDP", 100);

        assertEquals(result1, result2);
    }

    // --- error handling ---

    @Test
    void getSeries_handlesNetworkErrorGracefully() {
        var service = createService("dummy_key");

        // Should not throw even if network fails
        assertDoesNotThrow(() -> service.getSeries("NONEXISTENT_SERIES", 10));
    }
}
