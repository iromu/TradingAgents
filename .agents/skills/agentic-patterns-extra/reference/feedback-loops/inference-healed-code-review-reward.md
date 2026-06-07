---
title: "Inference-Healed Code Review Reward"
status: "proposed"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Open Source Agent RL Talk", "Will Brown (Prime Intellect)"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/inference-healed-code-review-reward.md"
tags: [reward-modeling, code-review, inference-healing, chain-of-thought, quality-assessment]
---

## Problem

Simple reward functions that only check for "all tests passed" fail to capture nuanced code quality issues (e.g.,
performance regressions, style violations, missing edge-case handling). A single binary signal at the end cannot guide
the agent to produce maintainable, high-quality code.

## Solution

Use an **inference-healed reward model**—a code-review critic that:

**1. Decomposes Code Quality into Subcriteria**

- **Correctness:** Does the code pass all tests?
- **Style:** Are linters satisfied?
- **Performance:** Are there clear performance regressions?
- **Security:** Does a static analyzer flag critical issues?

**2. Runs Internal Chain-of-Thought (CoT) Reasoning**

- If uncertain about a subcriterion, the critic runs a short CoT inside itself
- This "inference healing" allows the reward model to **explain** each sub-score

**3. Aggregates Subscores**

- Each subcriterion returns a float ∈ [0, 1]
- A weighted sum yields the final code-review score

**4. Generates Human-Readable Feedback**

- Alongside a numerical score, return a short analysis explaining issues

## Evidence

- **Evidence Grade:** `proposed`
- **Industry Validation:** Multi-criteria code review deployed at scale by Microsoft (600K+ PRs/month), Tencent (325M
  lines/month), Tekion (60% faster time to merge)

## How to use it

- **Critic Dataset Collection:** Gather examples of good vs. bad code patches
- **Critic Training:** Fine-tune a small LLM (1–2 B parameters) to produce sub-scores and CoT justifications
- **Integration into RL Loop:** Replace or augment the existing binary "tests-passed" reward
- **Selective Healing:** Generate CoT explanations only for subscores below a threshold

## Trade-offs

- **Pros:** Explainable feedback, higher code quality (performance, security included).
- **Cons:** Compute overhead per reward invocation, critic maintenance as standards evolve.

## References

- Derived from "inference healing" in reward modeling (Open Source Agent RL talk, May 2025)
- "Criterion-Led Reward Models" (DeepMind blog, April 2025)
- CodeRL: Mastering Code Generation through Pretrained Models and Deep Reinforcement Learning (NeurIPS 2022)
- Primary source: https://www.youtube.com/watch?v=Xkwok_XXQgw
