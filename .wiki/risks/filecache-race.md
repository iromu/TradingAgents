---
title: "FileCache Race Condition"
type: "risk"
status: "stale"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
updated_at: "2026-06-13"
---

# FileCache Race Condition

> **Status: Stale** — This risk was addressed in a later commit. The implementation changed from `ReentrantReadWriteLock` to per-key locking via `ConcurrentHashMap`.

## What Was Fixed

The original `getOrCompute()` used `ReentrantReadWriteLock` for the entire cache, which allowed a race window between the initial read check and the write lock acquisition. Two threads could both see a cache miss and both compute the value.

## Current Implementation

The fix uses per-key locking:

```java
private final Map<String, Object> lockMap = new ConcurrentHashMap<>();
```

Each unique cache key gets its own lock object. `getOrCompute()` uses `computeIfAbsent` on the lock map to ensure only one thread ever computes for a given key.

## Remaining Considerations

- File I/O itself is not atomic — if two processes (not just threads) write to the same cache file, the last write wins
- No TTL or expiration on cache entries — they persist indefinitely
- No cache size limit — the cache can grow unbounded
