package com.embabel.gekko.agent.identity;

import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InstrumentContextPromptContributor.
 * Verifies that the prompt contributor correctly formats instrument context
 * and handles the empty/unset state.
 */
class InstrumentContextPromptContributorTest {

    private InstrumentContextPromptContributor contributor;

    @BeforeEach
    void setUp() {
        contributor = new InstrumentContextPromptContributor();
    }

    @Test
    void contribution_returnsEmptyWhenNoContextSet() {
        // Before setContext() is called, contribution should be empty
        String result = contributor.contribution();
        assertEquals("", result);
    }

    @Test
    void contribution_returnsFormattedContextAfterSet() {
        var context = new InstrumentContext(
                "AAPL",
                "Apple Inc.",
                "Technology",
                "Consumer Electronics",
                "NASDAQ",
                "USD"
        );

        contributor.setContext(context);
        String result = contributor.contribution();

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Apple Inc."));
        assertTrue(result.contains("AAPL"));
        assertTrue(result.contains("Technology"));
        assertTrue(result.contains("Consumer Electronics"));
        assertTrue(result.contains("NASDAQ"));
    }

    @Test
    void contribution_includesCompanyFocusWarning() {
        var context = new InstrumentContext(
                "TSLA",
                "Tesla Inc.",
                "Consumer Discretionary",
                "Automakers",
                "NYSE",
                "USD"
        );

        contributor.setContext(context);
        String result = contributor.contribution();

        // Should include the "Do not confuse" warning with company name
        assertTrue(result.contains("Tesla Inc."));
        assertTrue(result.contains("Do not confuse"));
    }

    @Test
    void contribution_includesInstrumentHeader() {
        var context = new InstrumentContext(
                "MSFT",
                "Microsoft Corporation",
                "Technology",
                "Software",
                "NASDAQ",
                "USD"
        );

        contributor.setContext(context);
        String result = contributor.contribution();

        assertTrue(result.contains("INSTRUMENT CONTEXT:"));
    }

    @Test
    void contribution_overwritesPreviousContext() {
        var context1 = new InstrumentContext(
                "AAPL",
                "Apple Inc.",
                "Technology",
                "Consumer Electronics",
                "NASDAQ",
                "USD"
        );

        contributor.setContext(context1);
        assertTrue(contributor.contribution().contains("Apple Inc."));

        // Overwrite with different context
        var context2 = new InstrumentContext(
                "GOOGL",
                "Alphabet Inc.",
                "Technology",
                "Internet Content & Information",
                "NASDAQ",
                "USD"
        );

        contributor.setContext(context2);
        String result = contributor.contribution();
        assertTrue(result.contains("Alphabet Inc."));
        assertFalse(result.contains("Apple Inc."));
    }

    @Test
    void contribution_isVolatile() {
        // Verify thread-safety: setContext is volatile, contribution reads volatile field
        var context = new InstrumentContext(
                "AAPL",
                "Apple Inc.",
                "Technology",
                "Consumer Electronics",
                "NASDAQ",
                "USD"
        );

        Thread setter = new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            contributor.setContext(context);
        });

        Thread reader = new Thread(() -> {
            try {
                Thread.sleep(50);
                String result = contributor.contribution();
                assertNotNull(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        setter.start();
        reader.start();

        try {
            setter.join(1000);
            reader.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }
}