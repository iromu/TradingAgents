---
title: "Self-Critique Evaluator Loop"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Self-Taught Evaluators (Wang et al., 2024)", "Reflexion (Shinn et al., 2023)"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/self-critique-evaluator-loop.md"
tags: [self-critique, evaluator, synthetic-data, reward-modeling, self-taught]
---

## Problem

Human-labeled preference datasets are expensive to produce, slow to refresh, and quickly stale as base models and
domains change. Teams need scalable evaluation signals that can keep pace with model evolution without waiting on large
annotation cycles.

## Solution

Train a **self-taught evaluator** that bootstraps from synthetic data:

1. Generate multiple candidate outputs for an instruction.
2. Ask the model to judge and explain which is better (reasoning trace).
3. Fine-tune that judge on its own traces; iterate.
4. Use the judge as a reward model or quality gate for the main agent.
5. Periodically refresh with new synthetic debates to stay ahead of model drift.

**Dual-model variant (RLAIF):** Use a separate critic model to evaluate the generator, reducing bias at higher cost.

## Evidence

- **Evidence Grade:** `emerging`
- **Academic Source:** Wang et al., *Self-Taught Evaluators* (2024)

## How to use it

- Start with one narrow domain and define objective judge criteria
- Maintain a fixed holdout set with periodic human audits
- Use the evaluator as a gate first, then expand to reward-shaping
- Track disagreement rates between evaluator and human reviewers

## Trade-offs

- **Pros:** Scales evaluation coverage quickly, reduces dependence on expensive human labeling.
- **Cons:** Can overfit to synthetic preferences, needs careful anti-collusion safeguards.

## References

- Wang et al., *Self-Taught Evaluators* (2024)
- Shinn et al., *Reflexion: Language Agents with Verbal Reinforcement Learning* (2023)
- Bai et al., *Constitutional AI: Harmlessness from AI Feedback* (2022)
- Primary source: https://arxiv.org/abs/2408.02666
