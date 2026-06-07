---
title: "Prompt Caching via Exact Prefix Preservation"
status: "best-practice"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["OpenAI", "Anthropic", "HyperAgent"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/prompt-caching-via-exact-prefix-preservation.md"
tags: [prompt-caching, token-optimization, prefix-preservation, cost-reduction, zdr]
---

## Problem

Long-running agent conversations with many tool calls can suffer from **quadratic performance degradation**:

- **Growing JSON payloads**: Each iteration sends the entire conversation history to the API
- **Expensive re-computation**: Without caching, the model re-processes the same static content repeatedly
- **ZDR constraints**: Zero Data Retention (ZDR) policies prevent server-side state, ruling out `previous_response_id`
  optimization

## Solution

Maintain prompt cache efficiency through **exact prefix preservation** — always append new messages rather than
modifying existing ones, and carefully order messages to maximize cache hits.

**Core insight**: Prompt caches only work on **exact prefix matches**. If the first N tokens of a request match a
previous request, the cached computation can be reused.

**Message ordering strategy:**

1. **Static content first** (cached across all requests): system message, tool definitions, developer instructions
2. **Variable content last** (changes per request): user message, assistant messages, tool call results

**Configuration change via insertion**: When configuration changes mid-conversation, insert a new message rather than
modifying an existing one.

## Evidence

- **Evidence Grade:** `best-practice`
- **Production-validated savings:** 43% cost reduction demonstrated at scale (HyperAgent, 9.4B tokens/month)

## How to use it

**Prompt construction checklist:**

1. Order messages by stability: Static → Variable
2. Never modify existing messages: Always append new ones
3. Keep tool order consistent: Enumerate tools in deterministic order
4. Insert, don't update: For config changes, add new messages

**What breaks cache hits:** Changing the list of available tools, reordering messages, modifying existing message
content, changing the model.

## Trade-offs

**Pros:** Linear sampling cost, ZDR-compatible, no server state needed, simple conceptual model, production-validated
savings (43% cost reduction).
**Cons:** Quadratic network traffic, cache fragility with mid-conversation changes, disciplined ordering required, tool
enumeration complexity.

## References

* [Unrolling the Codex agent loop | OpenAI Blog](https://openai.com/index/unrolling-the-codex-agent-loop/)
* [Prompt Caching Documentation | OpenAI](https://platform.openai.com/docs/guides/prompt-caching)
* [Context Caching | Anthropic](https://docs.anthropic.com/en/docs/build-with-claude/context-caching)
* Related: [Context Window Auto-Compaction](context-window-auto-compaction.md)
