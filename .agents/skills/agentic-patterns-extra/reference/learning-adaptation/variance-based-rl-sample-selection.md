---
title: "Variance-Based RL Sample Selection"
status: "validated-in-production"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Theo (OpenAI Solutions Architect)", "Prashant (OpenAI RFT Team)"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/variance-based-rl-sample-selection.md"
tags: [reinforcement-learning, sample-efficiency, variance, data-quality, agent-rft]
---

## Problem

Not all training samples are equally valuable for reinforcement learning:

- **Zero-variance samples**: Model gets same score every time (always correct or always wrong) → no learning signal
- **Wasted compute**: Training on samples where the model has no uncertainty wastes expensive RL exploration
- **Poor data utilization**: With limited training budgets, you want to maximize learning from each sample

## Solution

**Run multiple baseline evaluations per sample to identify variance, then prioritize high-variance samples for training.
**

**The Variance Plot Methodology:**

1. **Baseline Evaluation:** Run your base model 3-5 times on each sample
2. **Visualize Variance:** Plot results to identify which samples have variance
3. **Categorize Samples:**
    - **Always correct** (variance = 0): Model already knows this
    - **Always incorrect** (variance = 0): Model can't learn this (too hard)
    - **Sometimes correct** (variance > 0): **Prime candidates for RL**
4. **Focus Training:** Prioritize or exclusively use high-variance samples

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Key Findings:**
    - Theo (OpenAI SA) on FinQA benchmark: ~85% of samples had zero variance, only ~15% could contribute to learning
    - Ambience Healthcare: F1 score 0.52 → 0.57 (+9.6%), 18% latency reduction
    - Cognition (Devon AI): 50% reduction in planning tool calls

## How to use it

**Step 1:** Run 3-5 baseline evaluations per sample on training/validation sets
**Step 2:** Create variance plot (X: sample index, Y: score, red crosses: best score, blue bars: mean ± std)
**Step 3:** Interpret: 15-30% high variance samples = good RL candidate; <10% = dataset may be too easy/hard
**Step 4:** Set compute multiplier based on variance level (low variance → higher multiplier for more exploration)
**Step 5:** Monitor variance evolution during training

## Trade-offs

**Pros:** Data efficiency, predictive (estimate improvement potential before training), diagnostic, guides
hyperparameters.
**Cons:** Upfront cost (3-5x baseline evals), small samples may have noisy variance, dynamic variance during training.

## References

- [OpenAI Build Hour: Agent RFT - Variance Analysis Demo (November 2025)](https://youtu.be/1s_7RMG4O4U)
- [Prioritized Experience Replay (Schaul et al., ICLR 2016)](https://arxiv.org/abs/1511.05952)
-
Related: [Agent Reinforcement Fine-Tuning](agent-reinforcement-fine-tuning.md), [Inference-Time Scaling](../orchestration-control/inference-time-scaling.md)
