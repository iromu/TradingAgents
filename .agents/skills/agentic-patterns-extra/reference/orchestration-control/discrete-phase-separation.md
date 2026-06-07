---
title: Discrete Phase Separation
status: emerging
authors:
  - Nikola Balic (@nibzard)
based_on:
  - Sam Stettner (Ambral)
category: Orchestration & Control
source: 'https://claude.com/blog/building-companies-with-claude-code'
tags:
  - orchestration
  - planning
  - research
  - context-management
  - multi-model
slug: discrete-phase-separation
id: discrete-phase-separation
summary: >-
  Break development workflows into isolated phases with clean handoffs. Each
  phase runs in a separate conversation, focusing exclusively on its objective.
updated_at: '2026-01-05'
---

## Problem

When AI agents attempt to simultaneously research, plan, and implement solutions, context contamination occurs.
Competing priorities within a single conversation degrade output quality as the agent struggles to balance exploration,
strategic thinking, and execution. This results in incomplete research, unclear plans, and suboptimal implementations.

## Solution

Break development workflows into isolated phases with clean handoffs between them. Each phase runs in a separate
conversation with a fresh context window, focusing exclusively on its objective:

**Research Phase (Opus 4.1):**

- Deep exploration of requirements, existing code, and constraints
- Comprehensive background investigation
- No implementation concerns

**Planning Phase (Opus 4.1):**

- Create structured implementation roadmap
- Define clear steps and dependencies
- No coding distractions

**Implementation Phase (Sonnet 4.5):**

- Execute each plan step systematically
- Focus purely on code quality and functionality
- Leverage the distilled outputs from previous phases

**Key principle:** Pass only distilled conclusions between phases, not full conversation history. This prevents context
pollution while maintaining necessary information flow.

```mermaid
graph LR
    A[Research Phase<br/>Opus 4.1] -->|Distilled Findings| B[Planning Phase<br/>Opus 4.1]
    B -->|Implementation Roadmap| C[Execution Phase<br/>Sonnet 4.5]

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#e8f5e9
```

## How to use it

**When to apply:**

- Complex features requiring significant background research
- Refactoring projects where understanding existing code is critical
- New codebases where architectural decisions need careful consideration
- Any task where mixing research and implementation degrades quality

**Implementation approach:**

1. **Research phase** - Start fresh conversation with a strong reasoning model
2. **Planning phase** - New conversation with distilled research findings
3. **Execution phase** - New conversation with the implementation plan

**Prerequisites:**

- Clear handoff documents between phases
- Discipline to resist combining phases
- Understanding of which model strengths to leverage

## Trade-offs

**Pros:**

- Higher quality outputs in each phase due to focused attention
- Prevents context contamination from competing objectives
- Deliberation before action improves tool use accuracy from 72% to 94% (Parisien et al. 2024)
- Leverages model-specific strengths (stronger models for reasoning, faster models for execution)
- Clearer mental model for complex projects

**Cons:**

- Requires more explicit phase management and handoffs
- Planning overhead adds ~35% latency
- Requires discipline to maintain phase boundaries
- Information loss risk if handoffs are poorly structured
- Higher total token usage across multiple conversations

## References

- [Building Companies with Claude Code](https://claude.com/blog/building-companies-with-claude-code) - Sam Stettner (
  Ambral) emphasizes: "Don't make Claude do research while it's trying to plan, while it's trying to implement."
- [Deliberation Before Action: Language Models with Tool Use](https://arxiv.org/abs/2403.05441) - Parisien et al., ICLR
  2024

---
