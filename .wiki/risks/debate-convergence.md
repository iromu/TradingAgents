---
title: "Debate Convergence"
type: "risk"
status: "mitigated"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/DebateLoopAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateAgent.java"
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
updated_at: "2026-07-06"
---

# Debate Convergence

## Status: Mitigated

The debate convergence risk has been **mitigated** — `DebateLoopAgent` now uses Jaccard similarity on bigrams to detect when positions have stabilized.

## How Convergence Works

`DebateLoopAgent` uses `RepeatUntilBuilder` with a convergence condition:

1. After each bull response, compute **Jaccard similarity** on bigrams between the current and previous bull response
2. If similarity ≥ `similarityThreshold` (default: 0.8), the debate stops
3. Falls back to `maxDebateIterations` (default: 5) as a safety net

### Bigram Jaccard Similarity

The algorithm:
1. Extracts bigrams (pairs of consecutive words) from each response
2. Computes Jaccard similarity: `|intersection| / |union|`
3. Returns a value between 0 (no overlap) and 1 (identical)

This is a lightweight, deterministic check that doesn't require an additional LLM call.

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `app.llm-options.similarity-threshold` | 0.8 | Jaccard similarity threshold for convergence |
| `app.llm-options.max-debate-iterations` | 5 | Safety net max rounds |

## Remaining Risks

1. **Threshold tuning:** A threshold of 0.8 may be too aggressive (stops too early) or too lenient (runs too long) depending on the ticker and market conditions
2. **Bigram limitations:** Bigram similarity doesn't capture semantic meaning — two responses could be semantically similar but use different wording (low similarity) or use similar wording but different conclusions (high similarity)
3. **Bull-only comparison:** Only bull responses are compared; bear responses are not checked for convergence
4. **Edge cases:** Very short responses may produce few bigrams, leading to unreliable similarity scores

## Impact

- **Too few rounds:** May miss important counterarguments
- **Too many rounds:** Wastes tokens on repetitive arguments
- **Convergence detection:** Reduces token cost when the debate stabilizes early

## Related

- `[[trading-workflow]]` — where the debate fits in the overall flow
- `[[investment-debate]]` — the debate feature page
- `[[agent-configuration]]` — where to adjust threshold and iteration count