---
title: "Memory Reinforcement Learning (MemRL)"
status: "emerging"
authors: ["Shengtao Zhang, Jiaqian Wang, et al."]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/memory-reinforcement-learning-memrl.md"
tags: [reinforcement-learning, memory, episodic, self-evolution, utility-ranking]
---

## Problem

LLMs struggle with **runtime self-evolution** due to the stability-plasticity dilemma:

- **Fine-tuning**: Computationally expensive and prone to catastrophic forgetting
- **RAG/memory systems**: Rely on semantic similarity that retrieves noise
- **No utility learning**: Can't distinguish high-value strategies from semantically similar but ineffective ones

## Solution

**MemRL** transfers reinforcement learning from parameter space to context space: instead of updating model weights, it
learns utility scores on episodic memories.

**Memory triplet structure:**

- **Intent**: What the user asked for (embedded)
- **Experience**: What the agent tried (solution trace)
- **Utility**: How well it worked (learned score, updated over time)

**Two-phase retrieval:**

1. **Phase A - Semantic filter**: Find semantically similar memories
2. **Phase B - Utility ranking**: Re-rank by learned utility scores

This filters out "distractor" memories that look relevant but historically lead to poor outcomes.

## Evidence

- **Evidence Grade:** `emerging`
- **Academic Source:** [Self-Evolving Agents via Runtime RL on Episodic Memory](https://arxiv.org/html/2601.03192v1) (
  2025)
- **Foundation:** Neural Episodic Control (Pritzel et al., 2017), Reflexion (Shinn et al., 2023)

## How to use it

1. Store experiences with utility scores in a memory bank
2. Retrieve with utility ranking: first filter by similarity, then re-rank by utility
3. Update utilities based on outcomes: `mem.utility += learning_rate * (reward - mem.utility)`

**When to use:** Multi-step tasks with clear success signals, reusable problem-solving patterns, can't afford
fine-tuning.
**When NOT to use:** Single-turn queries, no clear reward signals, highly diverse tasks.

## Trade-offs

**Pros:** No catastrophic forgetting (frozen LLM), self-improves from experience, filters out "look-alike" bad
solutions, no retraining needed.
**Cons:** Need reliable success/failure signals, memory overhead grows over time, cold start needs episodes.

## References

* [Self-Evolving Agents via Runtime RL on Episodic Memory](https://arxiv.org/html/2601.03192v1) - Zhang et al. (2025)
* [Neural Episodic Control](https://arxiv.org/abs/1703.01988) - Pritzel et al. (2017)
* [Reflexion: Language Agents with Verbal RL](https://arxiv.org/abs/2303.11366) - Shinn et al. (2023)
*
Related: [Episodic Memory Retrieval & Injection](../context-memory/episodic-memory-retrieval.md), [Agent Reinforcement Fine-Tuning](agent-reinforcement-fine-tuning.md)
