package com.embabel.gekko.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndRetrieveString() {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        cache.save("test_key", "hello world");

        String result = cache.get("test_key", String.class);

        assertNotNull(result);
        assertEquals("hello world", result);
    }

    @Test
    void getOrCompute_cachesResult() {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        int[] callCount = {0};
        String result1 = cache.getOrCompute("compute_key", String.class, () -> {
            callCount[0]++;
            return "computed_" + callCount[0];
        });

        assertEquals("computed_1", result1);
        assertEquals(1, callCount[0]);

        // Second call should use cache
        String result2 = cache.getOrCompute("compute_key", String.class, () -> {
            callCount[0]++;
            return "computed_" + callCount[0];
        });

        assertEquals("computed_1", result2);
        assertEquals(1, callCount[0]); // Supplier should not have been called again
    }

    @Test
    void sanitizeKey_rejectsNull() {
        FileCache cache = new FileCache();

        assertThrows(IllegalArgumentException.class, () -> cache.getOrCompute(null, String.class, () -> "null_key"));
    }

    @Test
    void sanitizeKey_rejectsBlank() {
        FileCache cache = new FileCache();

        assertThrows(IllegalArgumentException.class, () -> cache.getOrCompute("   ", String.class, () -> "blank_key"));
    }

    @Test
    void sanitizeKey_preservesSpecialChars() {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        // Special characters are preserved — the SHA-256 hash prevents traversal
        String result = cache.getOrCompute("a.b/c\\d\0", String.class, () -> "safe");

        assertNotNull(result);
        assertEquals("safe", result);
    }

    @Test
    void get_returnsNullForMissingKey() {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        assertNull(cache.get("nonexistent_key", String.class));
    }

    @Test
    void concurrent_sameKey_computeOnce() throws InterruptedException {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        AtomicInteger callCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Launch multiple threads requesting the same key simultaneously
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // All threads start at once
                    cache.getOrCompute("concurrent_key", String.class, () -> {
                        callCount.incrementAndGet();
                        try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }
                        return "computed";
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Release all threads at once
        startLatch.countDown();
        doneLatch.await();

        // Supplier should have been called exactly once
        assertEquals(1, callCount.get(), "Supplier should compute exactly once for same key");
    }

    @Test
    void concurrent_differentKeys_computeIndependently() throws InterruptedException {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        AtomicInteger callCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    cache.getOrCompute("key_" + idx, String.class, () -> {
                        callCount.incrementAndGet();
                        return "value_" + idx;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // Each key should have been computed once
        assertEquals(threadCount, callCount.get(), "Each different key should compute once");
    }

    @Test
    void concurrent_exceptionReleasesLock() throws InterruptedException {
        FileCache cache = new FileCache();
        ReflectionTestUtils.setField(cache, "baseDir", tempDir);

        // First call throws exception
        assertThrows(RuntimeException.class, () ->
                cache.getOrCompute("failing_key", String.class, () -> {
                    throw new RuntimeException("Intentional failure");
                }));

        // Second call with same key should succeed (lock was released)
        String result = cache.getOrCompute("failing_key", String.class, () -> "recovered");
        assertEquals("recovered", result);
    }
}
