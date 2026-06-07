---
title: "Agent Reinforcement Fine-Tuning (Agent RFT)"
status: "validated-in-production"
authors: ["OpenAI RFT Team", "Theo (OpenAI Solutions Architect)"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/agent-reinforcement-fine-tuning.md"
tags: [reinforcement-learning, fine-tuning, agent-rft, tool-use, optimization]
---

## Problem

After optimizing prompts and task design, agents may still underperform on specific business tasks because:

- **Domain shift**: Tools and business context differ from what the base model was trained on
- **Inefficient tool use**: Agents make too many tool calls or use wrong tools
- **Suboptimal reasoning**: The model doesn't reason well across specific tool outputs

## Solution

**Agent Reinforcement Fine-Tuning (Agent RFT)** trains model weights end-to-end on agentic tasks:

1. **Explore via actual tool calls** — During training rollouts, the agent calls real tool endpoints
2. **Receive custom reward signals** — Define what "good" looks like via flexible graders
3. **Learn multi-step reasoning** — The agent learns to reason across tool outputs
4. **Optimize for your metrics** — Reduce tool calls, improve accuracy, or balance both

**Grader Design Best Practices:**

- Use gradient rewards (0-1 floats, not binary 0/1) for clearer learning signals
- Prevent reward hacking: evaluate reasoning process, not just final answers
- Align with domain knowledge: measure grader-human consistency before training

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Production Implementations:** OpenAI Agent RFT, Ambience Healthcare (ICD-10 coding: F1 0.52→0.57), Cognition (50%
  reduction in planning tool calls)

## How to use it

1. **Baseline evaluation:** Run base model multiple times per sample to measure variance
2. **Host tools:** Deploy tool endpoints (FastAPI, Modal, etc.) that mirror production
3. **Design grader:** Create reward function that's hard to game, provides gradient
4. **Monitor training:** Watch reward curves, tool call distributions, reasoning token counts
5. **Evaluate results:** Compare fine-tuned model on validation set

## Trade-offs

**Pros:** End-to-end optimization, sample efficient (100-1000 samples), flexible rewards, natural speedups, domain
adaptation.
**Cons:** Infrastructure complexity, bursty traffic during training, grader design effort, debugging difficulty.

## References

- [OpenAI Build Hour: Agent RFT (November 2025)](https://youtu.be/1s_7RMG4O4U)
- [OpenAI Fine-tuning Guide](https://platform.openai.com/docs/guides/fine-tuning)
- ReAct: Synergizing Reasoning and Acting (Yao et al., ICLR 2023)
- Reflexion: Language Agents with Verbal RL (Shinn et al., NeurIPS 2023)
-
Related: [Variance-Based RL Sample Selection](../learning-adaptation/variance-based-rl-sample-selection.md), [Memory Reinforcement Learning](../learning-adaptation/memory-reinforcement-learning.md)
