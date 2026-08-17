---
title: "File Cache"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
  - "src/main/java/com/embabel/gekko/util/ResultCache.java"
  - "src/test/java/com/embabel/gekko/util/FileCacheTest.java"
  - "src/test/java/com/embabel/gekko/util/ResultCacheTest.java"
updated_at: "2026-08-17"
---

# File Cache

`FileCache` is a disk-based caching layer used throughout Gekko to cache LLM outputs and API responses. `ResultCache` is the higher-level contract that production code uses — it wraps `FileCache` and adds key normalization, error-payload guarding, and per-category TTL.

## ResultCache (Primary Contract)

Production code should use `ResultCache`, not raw `FileCache`. It adds three layers on top of disk storage:

### Key Normalization

`ResultCache.canonicalKey(category, parts...)` builds deterministic cache keys:
- The category is lowercased; all parts are uppercased
- Parts are joined with the ASCII unit separator (`\u001F`)
- This ensures `aapl`, `AAPL`, and `Aapl` all hit the same cache entry

### Error-Payload Guard

`ResultCache.isErrorPayload(value)` detects error, rate-limit, or empty responses from external APIs. These are **never persisted** to cache — only valid data results are cached. This prevents stale error payloads from masking subsequent valid API responses.

### Per-Category TTL

TTLs are configured in `application.yaml` under `app.cache.ttl`:
- `quote` — time-sensitive data (prices, quotes); defaults to `5m`
- `external-http` — API responses; defaults to `1h`
- LLM results have no TTL (they are deterministic for a given prompt)

On JVM restart, in-memory timestamps are lost, so `getWithTtl` treats any cached value without a recorded write time as expired (safe behavior).

## FileCache (Low-Level)

### Location

Cache files are stored in `data/llm/cache/` relative to the project root.

## Key Handling

### Validation

Cache keys are validated to be non-null and non-blank. Path traversal prevention relies entirely on the SHA-256 hash — the raw key is hashed before being used as a filename, so special characters like `.`, `/`, and `\` cannot cause traversal. Stripping these characters was removed in a past refactor because it caused hash collisions (e.g., `"a.b"` and `"ab"` produced identical keys after stripping).

### Hashing

Validated keys are hashed using SHA-256. The hash is used as the filename:
```
data/llm/cache/<sha256_hash>.json
data/llm/cache/<sha256_hash>.md
```

This prevents filename collisions, keeps filenames deterministic, and eliminates path traversal risks.

## Write Pattern

Both JSON and Markdown writes use **atomic writes** — data is written to a `.tmp` file first, then atomically renamed to the final path via `Files.move(REPLACE_EXISTING)`. This prevents partial read corruption if the process crashes mid-write.

## Thread Safety

Uses per-key locking via `ConcurrentHashMap<String, Object>`:
- Each unique cache key gets its own lock object
- `getOrCompute()` uses `computeIfAbsent` + double-check pattern
- Two threads requesting the same key compute exactly once
- Different keys compute independently
- Lock is cleaned up after successful computation via `lockMap.remove(key, lock)` (atomic removal)

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

## Safe to Clear

Both `data/llm/cache/` and `data/alphavantage/` may be **safely deleted at any time** after deployment. Stale cache entries are invalidated by:

- **TTL expiry** — `ResultCache` treats expired entries as cache misses and recomputes
- **Key shape changes** — after a code upgrade changes the `canonicalKey()` shape, old entries are never hit
- **Missing in-memory timestamp** — on JVM restart, any cached value without a recorded write time is treated as expired

Deleting the directories forces a full recomputation on next request but causes no data loss or corruption.