---
title: "FileCache Race Condition"
type: "risk"
status: "mitigated"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
  - "src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryRepository.java"
updated_at: "2026-08-13"
---

# FileCache Race Condition

> **Status: Mitigated** — `FileCache` uses per-key locking within a process. `DecisionMemoryRepository` uses cross-process FileLock.

## FileCache (in-process only)

`FileCache` uses per-key locking via `ConcurrentHashMap<String, Object>`:

- Each unique cache key gets its own lock object
- `getOrCompute()` uses `computeIfAbsent` + double-check pattern
- Two threads requesting the same key compute exactly once
- Different keys compute independently
- Lock is cleaned up after successful computation via `lockMap.remove(key, lock)` (atomic removal)
- Both JSON and Markdown writes use atomic writes (temp file + rename)
- **No cross-process protection** — if multiple JVM instances write to the same cache directory, the last write wins

## DecisionMemoryRepository (cross-process)

`DecisionMemoryRepository` has full cross-process protection via `FileLock`:

- Uses `FileChannel` + `FileLock` on a dedicated `.lock` file
- All read-modify-write operations (`appendPending()`, `resolve()`, `rotate()`, `recoverFromCorruption()`) are wrapped in `withLock()`
- Lock is acquired before reading, held during the modification, and released after the atomic write
- Fail-open on lock acquisition failure — logs error and throws RuntimeException

## Remaining Considerations

- **FileCache cross-process:** No cross-process protection for LLM response cache
- **No TTL or expiration:** Cache entries persist indefinitely
- **No cache size limit:** The cache can grow unbounded
