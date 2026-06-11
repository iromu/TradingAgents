---
title: "File Cache"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
  - "src/test/java/com/embabel/gekko/util/FileCacheTest.java"
updated_at: "2026-06-11"
---

# File Cache

`FileCache` is a disk-based caching layer used throughout Gekko to cache LLM outputs and API responses.

## Location

Cache files are stored in `data/llm/cache/` relative to the project root.

## How It Works

### Key Sanitization

Cache keys are sanitized to prevent path traversal:
- Removes `..`, `/`, `\`, null bytes, and shell metacharacters (`;`, `&`, `|`, `*`, `?`, `<`, `>`, `'`, `"`)
- If the key is empty after sanitization, an exception is thrown

### Key Hashing

Sanitized keys are hashed using SHA-256. The hash is used as the filename:
```
data/llm/cache/<sha256_hash>.json
data/llm/cache/<sha256_hash>.md
```

This prevents filename collisions and keeps filenames deterministic.

### Read/Write Pattern

**Read:**
1. Acquire read lock
2. Check if `.json` file exists → deserialize
3. Check if `.md` file exists → read as string
4. Return null if neither exists

**Write:**
1. Acquire write lock
2. If value is a `Report`, save both JSON and Markdown
3. If value is a String, save as Markdown
4. Otherwise, save as JSON

### Thread Safety

Uses `ReentrantReadWriteLock`:
- Read lock for cache lookups (multiple readers can run concurrently)
- Write lock for cache saves (exclusive access)
- Double-checked locking in `getOrCompute()` to avoid redundant computation

## Usage Pattern

```java
// Get cached value, or compute and save it
Ticker ticker = cache.getOrCompute("AAPL_ticker", Ticker.class, () -> {
    // Expensive computation
    return computeTicker();
});
```

## Known Issues

See `[[risks/filecache-race]]` for details on a known race condition in `getOrCompute()`.
