package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.VendorRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsDataToolsTest {

    @Mock
    private VendorRouter vendorRouter;

    @Test
    void getNews_delegatesToRouter() {
        NewsDataTools tools = new NewsDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_news", "AAPL", "2026-06-01", "2026-06-13"))
                .thenReturn("[{\"title\": \"AAPL earnings beat\"}]");

        String result = tools.getNews("AAPL", "2026-06-01", "2026-06-13");

        assertEquals("[{\"title\": \"AAPL earnings beat\"}]", result);
        verify(vendorRouter).routeToVendor("get_news", "AAPL", "2026-06-01", "2026-06-13");
    }

    @Test
    void getGlobalNews_delegatesToRouter() {
        NewsDataTools tools = new NewsDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_global_news", "2026-06-13", 30, 5))
                .thenReturn("[{\"title\": \"Global market update\"}]");

        String result = tools.getGlobalNews("2026-06-13", 30, 5);

        assertEquals("[{\"title\": \"Global market update\"}]", result);
        verify(vendorRouter).routeToVendor("get_global_news", "2026-06-13", 30, 5);
    }

    @Test
    void getInsiderSentiment_delegatesToRouter() {
        NewsDataTools tools = new NewsDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_insider_sentiment", "AAPL", "2026-06-13"))
                .thenReturn("{\"sentiment\": \"positive\"}");

        String result = tools.getInsiderSentiment("AAPL", "2026-06-13");

        assertEquals("{\"sentiment\": \"positive\"}", result);
        verify(vendorRouter).routeToVendor("get_insider_sentiment", "AAPL", "2026-06-13");
    }

    @Test
    void getInsiderTransactions_delegatesToRouter() {
        NewsDataTools tools = new NewsDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_insider_transactions", "AAPL", "2026-06-13"))
                .thenReturn("[{\"name\": \"Tim Cook\", \"shares\": 50000}]");

        String result = tools.getInsiderTransactions("AAPL", "2026-06-13");

        assertEquals("[{\"name\": \"Tim Cook\", \"shares\": 50000}]", result);
        verify(vendorRouter).routeToVendor("get_insider_transactions", "AAPL", "2026-06-13");
    }

    @Test
    void getNews_propagatesRouterError() {
        NewsDataTools tools = new NewsDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_news", "INVALID", "2026-06-01", "2026-06-13"))
                .thenReturn("Error in VendorRouter for method 'get_news': Symbol not found");

        String result = tools.getNews("INVALID", "2026-06-01", "2026-06-13");

        assertNotNull(result);
        assertTrue(result.contains("Error"));
    }
}
