---
title: "Self-Identity Accumulation"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Generative Agents (Stanford 2023)", "MemGPT (UC Berkeley 2023)"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/self-identity-accumulation.md"
tags: [identity, self-improvement, dual-hook, session-lifecycle, personalized-behavior]
---

## Problem

AI agents lack continuous memory across sessions. Each conversation starts from zero, causing:

- **Lost familiarity**: The agent doesn't remember user preferences, goals, or working patterns
- **Repetitive explanations**: Users must re-explain context and preferences each session
- **Shallow relationships**: Agent cannot build deeper understanding of user's needs over time

## Solution

Implement **dual-hook architecture** for self-identity accumulation:

1. **SessionStart Hook**: Inject accumulated identity/profile at session start
2. **SessionEnd Hook**: Extract new insights and refine the profile after each session
3. **Identity Document**: A persistent file (e.g., `WHO_AM_I.md`, `SOUL.md`) that evolves over time

Profile structure typically includes: Project Goals, Preferences, Communication Style, Workflow Patterns, Boundaries.

## Evidence

- **Evidence Grade:** `emerging`
- **Academic Foundation:** Generative Agents (Stanford 2023), MemGPT (UC Berkeley 2023)

## How to use it

1. Create identity document with initial structure
2. Configure SessionStart hook to read and inject it
3. Configure SessionEnd hook to refine it with new insights
4. Include instructions for when/how to update

## Trade-offs

**Pros:** Continuous familiarity, deepening relationship, reduced friction, personalized behavior, transparent.
**Cons:** Staleness risk, overfitting to one user, context overhead, extraction noise, requires hooks infrastructure.

## References

* Based on personal bot WHO_AM_I system
* [Generative Agents: Interactive Simulacra of Human Behavior](https://arxiv.org/abs/2304.03442) - Park et al. (
  Stanford, 2023)
* [MemGPT: Towards LLMs as Operating Systems](https://arxiv.org/abs/2310.08560) - Packer et al. (UC Berkeley, 2023)
* [Claude Code Hooks Documentation](https://docs.anthropic.com/en/docs/claude-code/hooks)
