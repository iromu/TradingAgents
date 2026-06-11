---
title: "FileCache Race Condition"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
updated_at: "2026-06-11"
---

# FileCache Race Condition

## The Problem

`FileCache.getOrCompute()` has a known race condition. While it uses double-checked locking with write locks, there's a window between the initial read-lock check and acquiring the write lock where another thread could also compute the same value.

## How It Happens

1. Thread A checks cache (read lock) → miss
2. Thread B checks cache (read lock) → miss
3. Thread A acquires write lock → computes and saves
4. Thread B acquires write lock → computes again (unnecessarily) and overwrites

## Impact

- **Wasted computation** — duplicate LLM calls or API requests
- **Wasted tokens** — unnecessary LLM usage costs
- **Cache thrashing** — the same key is recomputed when it shouldn't be

## Current Mitigation

The double-checked locking after acquiring the write lock does prevent the **second thread from saving a stale value**, but it doesn't prevent the **unnecessary computation**.

## Better Approach

Use a `ConcurrentHashMap.computeIfAbsent()` pattern or a dedicated cache library (Caffeine) that handles this correctly.
