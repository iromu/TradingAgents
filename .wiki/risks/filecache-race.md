---
title: "FileCache Race Condition"
type: "risk"
status: "mitigated"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
updated_at: "2026-08-13"
---

# FileCache Race Condition

> **Status: Mitigated** — The per-key locking implementation prevents concurrent duplicate computation within a single process.

## Current Implementation

`FileCache` uses per-key locking via `ConcurrentHashMap<String, Object>`:

- Each unique cache key gets its own lock object
- `getOrCompute()` uses `computeIfAbsent` + double-check pattern
- Two threads requesting the same key compute exactly once
- Different keys compute independently
- Lock is cleaned up after successful computation via `lockMap.remove(key, lock)` (atomic removal)
- Both JSON and Markdown writes use atomic writes (temp file + rename)

## Remaining Considerations

- **Cross-process:** No cross-process protection — if multiple JVM instances write to the same cache directory, the last write wins
- **No TTL or expiration:** Cache entries persist indefinitely
- **No cache size limit:** The cache can grow unbounded
