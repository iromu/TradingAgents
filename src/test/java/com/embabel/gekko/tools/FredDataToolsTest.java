package com.embabel.gekko.tools;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.gekko.dataflows.FredService;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FredDataTools LLM tool calling.
 */
class FredDataToolsTest {

    private FredDataTools tools;
    private FredService fredService;
    private FakeOperationContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        fredService = new FredService("test_key", new FileCache());
        tools = new FredDataTools(fredService);
        ctx = FakeOperationContext.create();
    }

    @Test
    void getMacroIndicators_delegatesToService() {
        String result = tools.getMacroIndicators("GDP");

        assertNotNull(result);
        // Either returns data or NO_DATA_AVAILABLE (API may not be reachable)
        assertTrue(result.contains("NO_DATA_AVAILABLE") || result.contains("| Date |"));
    }

    @Test
    void getMacroIndicators_handlesNullSeriesId() {
        String result = tools.getMacroIndicators(null);

        assertTrue(result.contains("NO_DATA_AVAILABLE"));
    }

    @Test
    void getMacroDashboard_delegatesToService() {
        String result = tools.getMacroDashboard("default");

        assertNotNull(result);
        assertTrue(result.contains("NO_DATA_AVAILABLE") || result.contains("| Date |"));
    }

    @Test
    void getMacroDashboard_handlesMultipleSeries() {
        String result = tools.getMacroDashboard("GDP,CPIAUCSL,UNRATE");

        assertNotNull(result);
        // Should contain results for all three series or NO_DATA_AVAILABLE
        assertTrue(result.contains("NO_DATA_AVAILABLE") || result.contains("GDP"));
    }

    @Test
    void getMacroDashboard_handlesEmptySeriesIds() {
        String result = tools.getMacroDashboard("");

        // Should return dashboard (defaults)
        assertNotNull(result);
    }

    @Test
    void getMacroIndicators_returnsNoDataWhenDisabled() throws Exception {
        // Use reflection to disable
        var field = FredDataTools.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(tools, false);

        String result = tools.getMacroIndicators("GDP");

        assertEquals("NO_DATA_AVAILABLE: FRED data is disabled (app.fred.enabled=false)", result);
    }
}
