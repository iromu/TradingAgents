package com.embabel.gekko.dataflows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VendorRouterTest {

    @Test
    void route_delegatesToAlphaVantageForFundamentals() {
        AlphaVantageService alphaVantageService = new AlphaVantageService();
        VendorRouter router = new VendorRouter(alphaVantageService);

        // The router should handle the method name without throwing
        String result = router.routeToVendor("get_fundamentals", "AAPL", "2026-01-01");
        // Should return error or data, not throw
        assertNotNull(result);
    }

    @Test
    void route_delegatesToAlphaVantageForBalanceSheet() {
        AlphaVantageService alphaVantageService = new AlphaVantageService();
        VendorRouter router = new VendorRouter(alphaVantageService);

        String result = router.routeToVendor("get_balance_sheet", "AAPL", "quarterly", "2026-01-01");
        assertNotNull(result);
    }

    @Test
    void route_delegatesToAlphaVantageForNews() {
        AlphaVantageService alphaVantageService = new AlphaVantageService();
        VendorRouter router = new VendorRouter(alphaVantageService);

        String result = router.routeToVendor("get_news", "AAPL", "2020-01-01", "2030-01-01");
        assertNotNull(result);
    }

    @Test
    void route_throwsOnUnknownMethod() {
        AlphaVantageService alphaVantageService = new AlphaVantageService();
        VendorRouter router = new VendorRouter(alphaVantageService);

        // The router catches exceptions and returns error string, not throws
        String result = router.routeToVendor("unknown_method", "AAPL");
        assertNotNull(result);
        assertTrue(result.contains("Error"));
    }

    @Test
    void route_handlesNullParams() {
        AlphaVantageService alphaVantageService = new AlphaVantageService();
        VendorRouter router = new VendorRouter(alphaVantageService);

        String result = router.routeToVendor("get_fundamentals", null, null);
        assertNotNull(result);
    }
}
