---
title: "Frontier-Focused Development"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["AMP (Thorsten Ball, Quinn Slack)"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/frontier-focused-development.md"
tags: [frontier, state-of-the-art, model-selection, product-strategy]
---

## Problem

AI capabilities advance rapidly along predictable scaling laws—products optimized for today's models become obsolete in
months. Many teams waste time solving problems that frontier models already solve, or build products tied to specific
models that won't stay competitive.

## Solution

**Always target the frontier**—the state-of-the-art models—and design products that can rapidly evolve as the frontier
moves.

**Core principles:**

1. **No model selector**: Pick the best model for each use case
2. **Frontier or nothing**: Only build features that push boundaries
3. **Rapid evolution**: Expect to completely change your product every 3 months
4. **Subscription resistance**: Avoid being tied to one model's pricing structure

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
    - AMP (Anthropic): Opinionated frontier model choices without user-facing selectors
    - Claude Code, Cursor, v0.dev, Perplexity—all use opinionated frontier model choices

## How to use it

- Only build features that push the frontier
- Don't offer model selectors that trap users in the past
- Expect quarterly product evolution cycles
- Target early adopters and frontier users

## Trade-offs

**Pros:** Always at the frontier, rapid learning, future-proof, quality focus, innovation positioned.
**Cons:** Higher costs, smaller market, rapid change, exclusionary, uncertainty.

## References

* [Raising an Agent Episode 9: The Assistant is Dead, Long Live the Factory](https://www.youtube.com/watch?v=2wjnV6F2arc) -
  AMP (Thorsten Ball, Quinn Slack, 2025)
* Kaplan et al. (2020). [Scaling Laws for Neural Language Models](https://arxiv.org/abs/2001.08361)
* Wei et al. (2022). [Emergent Abilities of Large Language Models](https://arxiv.org/abs/2206.07682)
* Related: [Disposable Scaffolding Over Durable Features](../orchestration-control/disposable-scaffolding.md)
