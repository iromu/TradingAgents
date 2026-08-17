package com.embabel.gekko.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ResultCacheTest {

    @TempDir
    Path tempDir;

    private FileCache fileCache;
    private ResultCache resultCache;

    @BeforeEach
    void setUp() {
        fileCache = new FileCache(tempDir.resolve("cache"));
        resultCache = new ResultCache(fileCache, "1s", "1h");
    }

    @Test
    void canonicalKeyNormalizesFirstPartToUppercase() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "nvda", "fundamentals");
        assertThat(key).contains("NVDA");
    }

    @Test
    void canonicalKeyIncludesAllParts() {
        String key1 = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "NVDA", "risk", "HIGH");
        String key2 = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "NVDA", "risk", "LOW");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void canonicalKeyIsCaseInsensitiveForSymbol() {
        String keyLower = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "nvda");
        String keyUpper = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "NVDA");
        assertThat(keyLower).isEqualTo(keyUpper);
    }

    @Test
    void normalizeSymbolUppercases() {
        assertThat(ResultCache.normalizeSymbol("brk.b")).isEqualTo("BRK.B");
        assertThat(ResultCache.normalizeSymbol(null)).isEmpty();
    }

    @Test
    void errorPayloadIsNotCached() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "NVDA", "quote");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("{\"error\": \"rate limit exceeded\"}");

        Object result = resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, key, String.class, supplier);
        assertThat(result).isEqualTo("{\"error\": \"rate limit exceeded\"}");
        verify(supplier, times(1)).get();
    }

    @Test
    void emptyStringIsNotCached() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "NVDA", "empty");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("");

        Object result = resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, key, String.class, supplier);
        assertThat((String) result).isEmpty();
    }

    @Test
    void nullPayloadIsNotCached() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "NVDA", "null");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(null);

        Object result = resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, key, String.class, supplier);
        assertThat(result).isNull();
    }

    @Test
    void successfulPayloadIsCached() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "NVDA", "report");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("A valid research report about NVDA.");

        Object first = resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, String.class, supplier);
        assertThat(first).isEqualTo("A valid research report about NVDA.");

        // Second call should hit the cache (supplier not invoked again)
        Object second = resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, String.class, supplier);
        assertThat(second).isEqualTo(first);
        verify(supplier, times(1)).get();
    }

    @Test
    void ttlExpiryCausesMiss() throws InterruptedException {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_QUOTE, "NVDA", "price");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("185.42");

        // First call: computes and caches
        resultCache.getOrCompute(ResultCache.CATEGORY_QUOTE, key, String.class, supplier);
        verify(supplier, times(1)).get();

        // Wait past the 1s TTL
        Thread.sleep(1_100);

        // Second call: TTL expired, should recompute
        resultCache.getOrCompute(ResultCache.CATEGORY_QUOTE, key, String.class, supplier);
        verify(supplier, times(2)).get();
    }

    @Test
    void freshEntryWithinTtlIsServedFromCache() {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_QUOTE, "NVDA", "price");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("185.42");

        resultCache.getOrCompute(ResultCache.CATEGORY_QUOTE, key, String.class, supplier);
        // Immediate second call — well within the 1s TTL
        Object cached = resultCache.get(ResultCache.CATEGORY_QUOTE, key, String.class);
        assertThat(cached).isEqualTo("185.42");
        verify(supplier, times(1)).get();
    }

    @Test
    void nonTimeSensitiveCategoryHasNoTtl() throws InterruptedException {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, "NVDA", "plan");
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("Investment plan content");

        resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, String.class, supplier);
        // Even after a delay, LLM category has no TTL — still served from cache
        Thread.sleep(1_100);
        Object cached = resultCache.get(ResultCache.CATEGORY_LLM, key, String.class);
        assertThat(cached).isEqualTo("Investment plan content");
        verify(supplier, times(1)).get();
    }
}
