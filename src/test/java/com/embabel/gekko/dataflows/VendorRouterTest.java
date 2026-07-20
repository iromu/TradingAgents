package com.embabel.gekko.dataflows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VendorRouterTest {

    @Test
    void route_delegatesToAlphaVantageForFundamentals() {
        AlphaVantageService alphaVantageService = mock(AlphaVantageService.class);
        when(alphaVantageService.getFundamentals(anyString(), anyString())).thenReturn("fundamentals data");
        VendorRouter router = new VendorRouter();
        router.setAlphaVantageService(alphaVantageService);

        String result = router.routeToVendor("get_fundamentals", "AAPL", "2026-01-01");
        assertEquals("fundamentals data", result);
    }

    @Test
    void route_delegatesToAlphaVantageForBalanceSheet() {
        AlphaVantageService alphaVantageService = mock(AlphaVantageService.class);
        when(alphaVantageService.getBalanceSheet(anyString(), anyString(), anyString())).thenReturn("balance sheet data");
        VendorRouter router = new VendorRouter();
        router.setAlphaVantageService(alphaVantageService);

        String result = router.routeToVendor("get_balance_sheet", "AAPL", "quarterly", "2026-01-01");
        assertEquals("balance sheet data", result);
    }

    @Test
    void route_delegatesToAlphaVantageForNews() {
        AlphaVantageService alphaVantageService = mock(AlphaVantageService.class);
        when(alphaVantageService.getNews(anyString(), anyString(), anyString())).thenReturn("news data");
        VendorRouter router = new VendorRouter();
        router.setAlphaVantageService(alphaVantageService);

        String result = router.routeToVendor("get_news", "AAPL", "2020-01-01", "2030-01-01");
        assertEquals("news data", result);
    }

    @Test
    void route_throwsOnUnknownMethod() {
        AlphaVantageService alphaVantageService = mock(AlphaVantageService.class);
        VendorRouter router = new VendorRouter();
        router.setAlphaVantageService(alphaVantageService);

        assertThrows(IllegalArgumentException.class, () -> router.routeToVendor("unknown_method", "AAPL"));
    }

    @Test
    void route_handlesNullParams() {
        AlphaVantageService alphaVantageService = mock(AlphaVantageService.class);
        // Null params should still route correctly (the service handles null internally)
        when(alphaVantageService.getFundamentals(isNull(), isNull())).thenReturn("fundamentals data");
        VendorRouter router = new VendorRouter();
        router.setAlphaVantageService(alphaVantageService);

        String result = router.routeToVendor("get_fundamentals", null, null);
        assertNotNull(result);
    }

    @Test
    void route_returnsErrorWhenAlphaVantageNotConfigured() {
        VendorRouter router = new VendorRouter();
        // No AlphaVantageService set

        String result = router.routeToVendor("get_fundamentals", "AAPL");
        assertNotNull(result);
        assertTrue(result.contains("not configured"));
    }
}
