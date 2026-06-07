---
title: "Self-Discover: LLM Self-Composed Reasoning Structures"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Self-Discover (NeurIPS 2024)"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/self-discover-reasoning-structures.md"
tags: [self-discover, reasoning-structures, meta-learning, self-composition]
---

## Problem

Fixed reasoning structures (CoT, ToT, GoT) may not be optimal for all tasks. Agents need to discover their own reasoning
structure for each problem type.

## Solution

**Self-Discover** enables LLMs to discover and compose their own reasoning structures dynamically. The model identifies
its strengths and weaknesses across different reasoning types, then selects or composes the optimal structure for each
problem.

## Evidence

- **Evidence Grade:** `emerging`
- **Academic Source:** Self-Discover paper (NeurIPS 2024)

## How to use it

- Enable the model to self-assess its reasoning capabilities
- Allow dynamic composition of reasoning structures
- Adapt structure based on problem type and difficulty

## Trade-offs

**Pros:** Adaptive reasoning, optimal structure selection, improved performance on diverse tasks.
**Cons:** Requires self-assessment capability, may add overhead for simple tasks.

## References

* Self-Discover paper (NeurIPS 2024)
*
Related: [Graph of Thoughts](graph-of-thoughts.md), [Tree-of-Thought Reasoning](../orchestration-control/tree-of-thought-reasoning.md)
