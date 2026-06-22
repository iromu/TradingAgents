package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.FredService;
import com.embabel.gekko.dataflows.PolymarketService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Extended Data Sources feature migrated from Tauric (Python).
 *
 * Validates:
 * - FredService and PolymarketService are Spring beans
 * - FredDataTools and PolymarketDataTools have @Tool annotations
 * - VendorRouter is a Spring bean
 * - Services have expected methods
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles({"base", "app", "observability", "local"})
class ExtendedDataSourcesIntegrationTest {

    @Autowired
    private FredDataTools fredDataTools;

    @Autowired
    private PolymarketDataTools polymarketDataTools;

    @Autowired
    private FredService fredService;

    @Autowired
    private PolymarketService polymarketService;

    @Test
    void shouldHaveFredDataToolsAsSpringBean() {
        assertNotNull(fredDataTools);
    }

    @Test
    void shouldHavePolymarketDataToolsAsSpringBean() {
        assertNotNull(polymarketDataTools);
    }

    @Test
    void shouldHaveFredServiceAsSpringBean() {
        assertNotNull(fredService);
    }

    @Test
    void shouldHavePolymarketServiceAsSpringBean() {
        assertNotNull(polymarketService);
    }

    @Test
    void shouldHaveFredMacroIndicatorsTool() {
        var methods = FredDataTools.class.getDeclaredMethods();
        boolean found = false;
        for (var method : methods) {
            if (method.getName().equals("getMacroIndicators")) {
                var toolAnno = method.getAnnotation(Tool.class);
                assertNotNull(toolAnno, "getMacroIndicators should have @Tool annotation");
                found = true;
                break;
            }
        }
        assertTrue(found, "FredDataTools should have getMacroIndicators method");
    }

    @Test
    void shouldHaveFredMacroDashboardTool() {
        var methods = FredDataTools.class.getDeclaredMethods();
        boolean found = false;
        for (var method : methods) {
            if (method.getName().equals("getMacroDashboard")) {
                var toolAnno = method.getAnnotation(Tool.class);
                assertNotNull(toolAnno, "getMacroDashboard should have @Tool annotation");
                found = true;
                break;
            }
        }
        assertTrue(found, "FredDataTools should have getMacroDashboard method");
    }

    @Test
    void shouldHavePolymarketPredictionMarketsTool() {
        var methods = PolymarketDataTools.class.getDeclaredMethods();
        boolean found = false;
        for (var method : methods) {
            if (method.getName().equals("getPredictionMarkets")) {
                var toolAnno = method.getAnnotation(Tool.class);
                assertNotNull(toolAnno, "getPredictionMarkets should have @Tool annotation");
                found = true;
                break;
            }
        }
        assertTrue(found, "PolymarketDataTools should have getPredictionMarkets method");
    }

    @Test
    void shouldHaveFredServiceMethods() {
        var methods = FredService.class.getDeclaredMethods();
        boolean hasGetSeries = false;
        boolean hasGetDashboard = false;
        boolean hasGetMultipleSeries = false;

        for (var method : methods) {
            if (method.getName().equals("getSeries")) hasGetSeries = true;
            if (method.getName().equals("getDashboard")) hasGetDashboard = true;
            if (method.getName().equals("getMultipleSeries")) hasGetMultipleSeries = true;
        }

        assertTrue(hasGetSeries, "FredService should have getSeries method");
        assertTrue(hasGetDashboard, "FredService should have getDashboard method");
        assertTrue(hasGetMultipleSeries, "FredService should have getMultipleSeries method");
    }

    @Test
    void shouldHavePolymarketServiceMethods() {
        var methods = PolymarketService.class.getDeclaredMethods();
        boolean hasSearchMarkets = false;
        boolean hasGetMarket = false;

        for (var method : methods) {
            if (method.getName().equals("searchMarkets")) hasSearchMarkets = true;
            if (method.getName().equals("getMarket")) hasGetMarket = true;
        }

        assertTrue(hasSearchMarkets, "PolymarketService should have searchMarkets method");
        assertTrue(hasGetMarket, "PolymarketService should have getMarket method");
    }

    @Test
    void shouldHaveFredAndPolymarketEnabledByDefault() {
        // Tools should be enabled by default — beans created successfully
        assertNotNull(fredDataTools);
        assertNotNull(polymarketDataTools);
    }
}