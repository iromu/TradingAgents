---
title: "Progressive Disclosure for Large Files"
status: "validated-in-production"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Will Larson (2025)", "LangChain"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/progressive-disclosure-large-files.md"
tags: [large-files, progressive-disclosure, file-loading, token-optimization]
---

## Problem

Large files (PDFs, DOCXs, images) overwhelm the context window when loaded naively. A 5-10MB PDF may contain only
10-20KB of relevant text/tables, but the entire file is often shoved into context, wasting tokens and degrading
performance.

## Solution

Apply **progressive disclosure**: load file metadata first, then provide tools to load content on-demand.

**Core approach:**

1. **Always include file metadata** in the prompt (not full content)
2. **Optionally preload first N KB** of appropriate mimetypes (configurable per-workflow)
3. **Provide three file operations:**
    - `load_file(id)` - Load entire file into context
    - `peek_file(id, start, stop)` - Load a section of file
    - `extract_file(id)` - Transform PDF/DOCX/PPT into simplified text
4. **Include a `large_files` skill** explaining when/how to use these tools

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Production Implementation:** Will Larson's internal agent (2025)

## How to use it

- **Best for:** Document comparison workflows, ticket systems with file attachments, data export analysis
- **Implementation:** File `id` should be a stable reference for tool calls
- **Cache extracted content** to avoid re-processing (TTL: text 24h, tables 7 days, metadata 1h)

## Trade-offs

**Pros:** Enables working with files much larger than context window, agent has control over what/when to load, reusable
across workflows, extracted content is often 100x smaller.
**Cons:** Adds tool call overhead, requires preloading heuristics, extraction from complex formats can be slow.

## References

* [Building an internal agent: Progressive disclosure and handling large files](https://lethain.com/agents-large-files/) -
  Will Larson (2025)
* Related: [Progressive Tool Discovery](../tool-use-environment/progressive-tool-discovery.md)
* Related: [Context-Minimization Pattern](context-minimization-pattern.md)
* Yang et al. (2016). "Hierarchical Attention Networks for Document Classification." NAACL
* LangChain - Document loaders with metadata-first approach
