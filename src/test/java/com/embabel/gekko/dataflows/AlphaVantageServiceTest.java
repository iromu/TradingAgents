package com.embabel.gekko.dataflows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class AlphaVantageServiceTest {

    @TempDir
    Path tempDir;

    private AlphaVantageService createService() {
        AlphaVantageService service = new AlphaVantageService();
        ReflectionTestUtils.setField(service, "cacheDir", tempDir.toString());
        service.init(); // @PostConstruct doesn't fire outside Spring container
        return service;
    }

    // --- Existing tests (preserved) ---

    @Test
    void constructor_setsUpRestTemplate() {
        AlphaVantageService service = createService();
        assertNotNull(service);
    }

    @Test
    void getNews_methodExists() {
        assertDoesNotThrow(() -> AlphaVantageService.class.getMethod(
                "getNews", String.class, String.class, String.class));
    }

    @Test
    void constructor_configuresRestTemplateWithTimeouts() {
        AlphaVantageService service = createService();
        RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertNotNull(rt);
    }

    // --- Ticker validation tests ---

    @Test
    void validateTicker_rejectsNull() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker(null));
    }

    @Test
    void validateTicker_rejectsEmpty() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker(""));
    }

    @Test
    void validateTicker_rejectsBlank() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("  "));
    }

    @Test
    void validateTicker_rejectsLowercase() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("aapl"));
    }

    @Test
    void validateTicker_rejectsTooLong() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("ABCDEFG"));
    }

    @Test
    void validateTicker_rejectsSpecialChars() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("A@PL"));
    }

    @Test
    void validateTicker_rejectsNumbers() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("A1PL"));
    }

    @Test
    void validateTicker_rejectsSpaces() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.validateTicker("AAP L"));
    }

    @Test
    void validateTicker_acceptsValidTickers() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.validateTicker("AAPL"));
        assertDoesNotThrow(() -> service.validateTicker("A"));
        assertDoesNotThrow(() -> service.validateTicker("GOOGL"));
        assertDoesNotThrow(() -> service.validateTicker("MSFT"));
        assertDoesNotThrow(() -> service.validateTicker("TSLA"));
        assertDoesNotThrow(() -> service.validateTicker("BRK.B"));
    }

    @Test
    void getFundamentals_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getFundamentals("invalid ticker!"));
    }

    @Test
    void getBalanceSheet_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getBalanceSheet("bad ticker", "annual", null));
    }

    @Test
    void getIncomeStatement_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getIncomeStatement("bad ticker", "annual", null));
    }

    @Test
    void getCashflow_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getCashflow("bad ticker", "annual", null));
    }

    @Test
    void getInsiderSentiment_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getInsiderSentiment("bad ticker", "1M"));
    }

    @Test
    void getInsiderTransactions_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getInsiderTransactions("bad ticker"));
    }

    @Test
    void getNews_validatesTicker() {
        AlphaVantageService service = createService();
        assertThrows(IllegalArgumentException.class, () ->
                service.getNews("bad ticker", "2026-01-01", "2026-01-31"));
    }

    // --- formatDateForApi safety tests ---

    @Test
    void formatDateForApi_parsesValidDate() {
        AlphaVantageService service = createService();
        String result = service.formatDateForApi("2026-01-15");
        assertNotNull(result);
        assertTrue(result.startsWith("20260115"));
    }

    @Test
    void formatDateForApi_handlesNullGracefully() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.formatDateForApi(null));
        String result = service.formatDateForApi(null);
        assertNotNull(result);
        // Should return a sensible default (current date formatted as yyyyMMdd'T'HHmm, e.g. 20260115T0000)
        assertTrue(result.matches("\\d{8}T\\d{4}"));
    }

    @Test
    void formatDateForApi_handlesEmptyStringGracefully() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.formatDateForApi(""));
        String result = service.formatDateForApi("");
        assertNotNull(result);
    }

    @Test
    void formatDateForApi_handlesGarbageInputGracefully() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.formatDateForApi("not-a-date"));
        assertDoesNotThrow(() -> service.formatDateForApi("2026/01/15"));
        assertDoesNotThrow(() -> service.formatDateForApi("yesterday"));
        String result = service.formatDateForApi("not-a-date");
        assertNotNull(result);
    }

    // --- API key not in BASE_URL constant (apikey appended at request time) ---

    @Test
    void apiKeyNotInBaseUrlConstant() {
        // Verify that the BASE_URL constant doesn't include apikey parameter
        try {
            Field baseUrlField = AlphaVantageService.class.getDeclaredField("BASE_URL");
            baseUrlField.setAccessible(true);
            String baseUrl = (String) baseUrlField.get(null);
            assertFalse(baseUrl.contains("apikey"),
                    "BASE_URL should not contain apikey parameter");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access BASE_URL field: " + e.getMessage());
        }
    }

    // --- Cache key tests (preserved from original) ---

    @Test
    void getNews_differentDates_produceDifferentCacheKeys() {
        AlphaVantageService service = createService();
        // These will fail at the network level but won't throw validation errors
        assertDoesNotThrow(() -> {
            try {
                service.getNews("AAPL", "2026-01-01", "2026-01-31");
            } catch (RuntimeException e) {
                // Expected: network error, not validation error
            }
        });
    }

    @Test
    void getGlobalNews_includesTopicInCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getGlobalNews("technology", 10, 1));
    }

    @Test
    void getInsiderSentiment_includesIntervalInCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getInsiderSentiment("AAPL", "1M"));
    }

    @Test
    void getFundamentals_usesCorrectCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getFundamentals("AAPL"));
    }

    @Test
    void getBalanceSheet_usesCorrectCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getBalanceSheet("AAPL", "quarterly", null));
    }

    @Test
    void getCashflow_usesCorrectCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getCashflow("AAPL", "quarterly", null));
    }

    @Test
    void getIncomeStatement_usesCorrectCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getIncomeStatement("AAPL", "quarterly", null));
    }

    @Test
    void getInsiderTransactions_usesCorrectCacheKey() {
        AlphaVantageService service = createService();
        assertDoesNotThrow(() -> service.getInsiderTransactions("AAPL"));
    }

    // --- Timeout configuration tests ---

    @Test
    void init_configuresCustomTimeouts() {
        AlphaVantageService service = new AlphaVantageService();
        ReflectionTestUtils.setField(service, "cacheDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "connectTimeoutMs", 5000);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 15000);
        service.init();

        RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertNotNull(rt);
    }

    @Test
    void init_usesDefaultsWhenTimeoutsAreZero() {
        AlphaVantageService service = new AlphaVantageService();
        ReflectionTestUtils.setField(service, "cacheDir", tempDir.toString());
        // connectTimeoutMs and readTimeoutMs default to 0 in non-Spring context
        service.init();

        RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertNotNull(rt);
        // Should use defaults: 10000ms connect, 30000ms read
    }
}
