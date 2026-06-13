package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.VendorRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundamentalDataToolsTest {

    @Mock
    private VendorRouter vendorRouter;

    @Test
    void getFundamentals_delegatesToRouter() {
        FundamentalDataTools tools = new FundamentalDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_fundamentals", "AAPL", "2026-06-13"))
                .thenReturn("{\"revenue\": 1000}");

        String result = tools.getFundamentals("AAPL", "2026-06-13");

        assertEquals("{\"revenue\": 1000}", result);
        verify(vendorRouter).routeToVendor("get_fundamentals", "AAPL", "2026-06-13");
    }

    @Test
    void getBalanceSheet_delegatesToRouter() {
        FundamentalDataTools tools = new FundamentalDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_balance_sheet", "AAPL", "annual", "2026-06-13"))
                .thenReturn("{\"assets\": 5000}");

        String result = tools.getBalanceSheet("AAPL", "annual", "2026-06-13");

        assertEquals("{\"assets\": 5000}", result);
        verify(vendorRouter).routeToVendor("get_balance_sheet", "AAPL", "annual", "2026-06-13");
    }

    @Test
    void getCashflow_delegatesToRouter() {
        FundamentalDataTools tools = new FundamentalDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_cashflow", "AAPL", "quarterly", "2026-06-13"))
                .thenReturn("{\"operatingCashFlow\": 500}");

        String result = tools.getCashflow("AAPL", "quarterly", "2026-06-13");

        assertEquals("{\"operatingCashFlow\": 500}", result);
        verify(vendorRouter).routeToVendor("get_cashflow", "AAPL", "quarterly", "2026-06-13");
    }

    @Test
    void getIncomeStatement_delegatesToRouter() {
        FundamentalDataTools tools = new FundamentalDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_income_statement", "AAPL", "annual", "2026-06-13"))
                .thenReturn("{\"netIncome\": 2000}");

        String result = tools.getIncomeStatement("AAPL", "annual", "2026-06-13");

        assertEquals("{\"netIncome\": 2000}", result);
        verify(vendorRouter).routeToVendor("get_income_statement", "AAPL", "annual", "2026-06-13");
    }

    @Test
    void getFundamentals_propagatesRouterError() {
        FundamentalDataTools tools = new FundamentalDataTools(vendorRouter);
        when(vendorRouter.routeToVendor("get_fundamentals", "INVALID", "2026-06-13"))
                .thenReturn("Error in VendorRouter for method 'get_fundamentals': Symbol not found");

        String result = tools.getFundamentals("INVALID", "2026-06-13");

        assertNotNull(result);
        assertTrue(result.contains("Error"));
    }
}
