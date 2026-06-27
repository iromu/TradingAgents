package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.PolymarketService;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolymarketDataTools LLM tool calling.
 */
class PolymarketDataToolsTest {

    private PolymarketDataTools tools;
    private PolymarketService polymarketService;

    @BeforeEach
    void setUp() throws Exception {
        Path cacheDir = Files.createTempDirectory("polymarket-tools-test-");
        var cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, cacheDir);
        polymarketService = new PolymarketService(cache);
        tools = new PolymarketDataTools(polymarketService);
    }

    @Test
    void getPredictionMarkets_delegatesToService() {
        String result = tools.getPredictionMarkets("Fed rate cut");

        assertNotNull(result);
        assertTrue(result.contains("NO_DATA_AVAILABLE") || result.contains("| Market |"));
    }

    @Test
    void getPredictionMarkets_handlesEmptyQuery() {
        String result = tools.getPredictionMarkets("");

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    @Test
    void getPredictionMarkets_handlesNullQuery() {
        String result = tools.getPredictionMarkets(null);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    @Test
    void getPredictionMarkets_returnsNoDataWhenDisabled() throws Exception {
        var field = PolymarketDataTools.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(tools, false);

        String result = tools.getPredictionMarkets("Fed rate cut");

        assertEquals("NO_DATA_AVAILABLE: Polymarket data is disabled (app.polymarket.enabled=false)", result);
    }
}
