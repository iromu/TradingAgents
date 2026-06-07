---
title: "Session-Scoped Context Runtime for Agent Tools"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["lean-ctx (yvgude)", "MCP Specification"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/session-scoped-context-runtime-for-agent-tools.md"
tags: [context-runtime, caching, tool-normalization, session-scoped]
---

## Problem

Coding agents read the same files and command outputs many times per session. Each hop typically pastes full text into
the model context, so cost and latency grow with repetition and verbosity even when the underlying artifact has not
changed.

## Solution

Introduce a **context runtime** alongside the agent—commonly as an MCP server—that owns how workspace state enters the
model:

1. **Session-scoped cache** for read operations with cheap revalidation (e.g., file mtime) so identical reads collapse
   to small cache hits
2. **Structured read modes** (dependency maps, signatures, diffs, task-filtered excerpts) so the agent requests the
   smallest representation that supports the next decision
3. **Normalized tool channels** so shell and search results pass through compressing adapters where patterns are stable

## Evidence

- **Evidence Grade:** `emerging`
- **Reference Implementation:** lean-ctx (Apache-2.0): https://github.com/yvgude/lean-ctx

## How to use it

- Expose read, search, shell, and tree operations through the runtime
- Default to a neutral automatic mode, then tighten to maps or signatures when the task only needs structure
- Invalidate or refresh on explicit edits, branch changes, or when the host signals a new subagent boundary
- Pair with existing context hygiene patterns (auto-compaction or minimization)

## Trade-offs

**Pros:** Fewer redundant tokens on repeated exploration, easier enforcement of "structure-first" context, centralized
policy.
**Cons:** Additional moving parts, cache staleness risk, host integration work.

## References

- lean-ctx (Apache-2.0 reference implementation): https://github.com/yvgude/lean-ctx
- Model Context Protocol specification: https://modelcontextprotocol.io/
-
Related: [MCP Pattern Injection](../tool-use-environment/mcp-pattern-injection.md), [Context-Minimization Pattern](context-minimization-pattern.md)
