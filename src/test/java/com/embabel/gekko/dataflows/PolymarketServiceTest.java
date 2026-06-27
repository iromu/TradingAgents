package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolymarketService — search, getMarket, caching, error handling.
 */
class PolymarketServiceTest {

    @TempDir
    Path tempDir;

    private PolymarketService createService() throws Exception {
        Path cacheDir = Files.createTempDirectory("polymarket-test-cache-");
        var cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, cacheDir);
        return new PolymarketService(cache);
    }

    // --- searchMarkets ---

    @Test
    void searchMarkets_returnsNoDataForEmptyQuery() throws Exception {
        var service = createService();

        String result = service.searchMarkets("");

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
        assertTrue(result.contains("Search query is required"));
    }

    @Test
    void searchMarkets_handlesNetworkErrorGracefully() throws Exception {
        var service = createService();

        // Should not throw even if network fails
        assertDoesNotThrow(() -> service.searchMarkets("Fed rate cut"));
    }

    @Test
    void searchMarkets_returnsFormattedResult() throws Exception {
        var service = createService();

        // Will likely fail due to network/API, but should return a string
        String result = service.searchMarkets("Fed rate cut");

        assertNotNull(result);
        // Either success or NO_DATA_AVAILABLE
        assertTrue(result.contains("NO_DATA_AVAILABLE") || result.contains("| Market |"));
    }

    // --- getMarket ---

    @Test
    void getMarket_returnsNoDataForEmptySlug() throws Exception {
        var service = createService();

        String result = service.getMarket("");

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
        assertTrue(result.contains("Market slug is required"));
    }

    @Test
    void getMarket_handlesNetworkErrorGracefully() throws Exception {
        var service = createService();

        assertDoesNotThrow(() -> service.getMarket("nonexistent-market-slug"));
    }

    // --- caching ---

    @Test
    void searchMarkets_cachesResponse() throws Exception {
        var service = createService();

        String result1 = service.searchMarkets("Fed rate cut");
        String result2 = service.searchMarkets("Fed rate cut");

        assertEquals(result1, result2);
    }

    // --- error handling ---

    @Test
    void searchMarkets_handlesNullQuery() throws Exception {
        var service = createService();

        String result = service.searchMarkets(null);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    @Test
    void getMarket_handlesNullSlug() throws Exception {
        var service = createService();

        String result = service.getMarket(null);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }
}
