---
title: "File Cache"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
  - "src/test/java/com/embabel/gekko/util/FileCacheTest.java"
updated_at: "2026-07-06"
---

# File Cache

`FileCache` is a disk-based caching layer used throughout Gekko to cache LLM outputs and API responses.

## Location

Cache files are stored in `data/llm/cache/` relative to the project root.

## Key Handling

### Sanitization

Cache keys are sanitized to prevent path traversal attacks:
- Strips only path traversal sequences (`..`, `/`, `\`, null bytes)
- If the key is empty after sanitization, an exception is thrown

### Hashing

Sanitized keys are hashed using SHA-256. The hash is used as the filename:
```
data/llm/cache/<sha256_hash>.json
data/llm/cache/<sha256_hash>.md
```

This prevents filename collisions and keeps filenames deterministic.

## Read/Write Pattern

**Read:**
1. Check if `.json` file exists → deserialize
2. Check if `.md` file exists → read as string
3. Return null if neither exists

**Write:**
1. If value is a `Report`, save both JSON and Markdown
2. If value is a String, save as Markdown
3. Otherwise, save as JSON
4. Uses **atomic write** — writes to a `.tmp` file first, then renames to the final path

## Thread Safety

Uses per-key locking via `ConcurrentHashMap<String, Object>`:
- Each unique cache key gets its own lock object
- `getOrCompute()` uses `computeIfAbsent` + double-check pattern
- Two threads requesting the same key compute exactly once
- Different keys compute independently
- Lock is cleaned up after successful computation

## Usage Pattern

```java
// Get cached value, or compute and save it
Ticker ticker = cache.getOrCompute("AAPL_ticker", Ticker.class, () -> {
    // Expensive computation
    return computeTicker();
});
```

## API

| Method | Returns | Description |
|--------|---------|-------------|
| `get(key, clazz)` | `T` (null if missing) | Get cached value by key |
| `getOrCompute(key, clazz, supplier)` | `T` | Get or compute with per-key locking |
| `save(key, value)` | void | Save a value to cache |

## Character Encoding

Uses `StandardCharsets.UTF_8` explicitly (not `Charset.defaultCharset()`).