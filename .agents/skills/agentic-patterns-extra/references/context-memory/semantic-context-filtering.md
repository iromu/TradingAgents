---
title: "Semantic Context Filtering Pattern"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Hyperbrowser Team (@hyperbrowserai)"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/semantic-context-filtering.md"
tags: [context-filtering, token-optimization, semantic-extraction, noise-reduction]
---

## Problem

Raw data sources are too verbose and noisy for effective LLM consumption. Full representations include invisible elements, implementation details, and irrelevant information that bloat context and confuse reasoning.

Research on boilerplate detection shows that **40-80% of web page content** is typically navigation, footers, ads, and other boilerplate that should be filtered before semantic processing (Kohlschütter et al., SIGIR 2010).

## Solution

Extract only the semantic, interactive, or relevant elements from raw data. Filter out noise and provide the LLM with a clean representation that captures what matters for reasoning.

**Core Principle: Don't send raw data to the LLM. Send semantic abstractions.**

**Example 1: Browser Accessibility Tree**
Instead of full HTML DOM (10,000+ tokens), extract the accessibility tree (100-200 tokens) with only interactive elements.

**Example 2: API Response Filtering**
Filter out internal metadata, request IDs, and debug info — keep only business-relevant fields.

**Example 3: Document Section Extraction**
Skip headers, footers, navigation, and boilerplate — extract only the actual content sections.

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
  - 10-100x token reduction through semantic filtering
  - Better LLM reasoning when focused on signal, not noise
  - Validated across browser automation (Puppeteer/Playwright), RAG frameworks, and code analysis tools

## How to use it

1. Identify semantic elements for your domain (interactive elements, relevant fields, content sections)
2. Build a filter layer that extracts only those elements
3. Apply filtering before every LLM call
4. Maintain reference mapping between filtered and original data for execution

## Trade-offs

**Pros:** Dramatic token reduction (10-100x), better LLM reasoning, lower costs, faster inference, higher reliability.
**Cons:** Filter complexity, information loss risk, domain-specific implementation, mapping overhead.

## References

- [HyperAgent GitHub Repository](https://github.com/hyperbrowserai/HyperAgent)
- Kohlschütter et al., ["Boilerplate Detection using Shallow Text Features"](https://doi.org/10.1145/1835449.1835550), SIGIR 2010
- Beurer-Kellner et al., ["Design Patterns for Securing LLM Agents"](https://arxiv.org/abs/2506.08837), arXiv 2025
- Related: [Context Window Anxiety Management](context-window-anxiety-management.md), [Context-Minimization Pattern](context-minimization-pattern.md)
