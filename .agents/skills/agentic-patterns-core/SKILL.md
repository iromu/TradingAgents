---
name: agentic-patterns-core
description: >-
  The canonical reference for 21 agentic design patterns distilled from
  promptadvisers/agentic-design-patterns-docs. Use this skill whenever the user
  asks about agentic architecture, how to structure an AI agent workflow, or
  wants to choose the right pattern for a use case. Triggers on questions about
  prompt chaining, routing, parallelization, reflection, tool use, planning,
  multi-agent collaboration, memory management, learning & adaptation, MCP,
  goal setting, exception handling, human-in-the-loop, RAG, agent-to-agent
  communication, resource-aware optimization, reasoning techniques, guardrails,
  evaluation & monitoring, prioritization, and exploration & discovery. Also
  trigger when the user describes an agent problem and needs pattern
  recommendations, wants to compare patterns, or needs implementation guidance
  for a specific pattern.
---

# Agentic Patterns Core

The canonical reference for **21 agentic design patterns** from
[promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs)
(MIT License). Each pattern includes when to use it, where it fits, pros and
cons, and real-world examples.

## Pattern Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CORE PATTERNS                             │
├─────────────────────────────────────────────────────────────────┤
│ 1. PROMPT CHAINING:    Task1 → Task2 → Task3 → Merge           │
│ 2. ROUTING:            Request → Router → Specialized Agent     │
│ 3. PARALLELIZATION:    Split → [W1|W2|W3|W4] → Merge           │
│ 4. REFLECTION:         Generate → Critique → Revise → Final     │
│ 5. TOOL USE:           Agent → Select Tool → Call → Process     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      ADVANCED PATTERNS                           │
├─────────────────────────────────────────────────────────────────┤
│ 6. PLANNING:           Goal → Milestones → Execute → Monitor    │
│ 7. MULTI-AGENT:        Coordinator → [A1|A2|A3|A4] → Output    │
│ 8. MEMORY:             Input → Classify → Store → Retrieve      │
│ 9. LEARNING:           Feedback → Learn → Test → Deploy         │
│ 10. MCP:               Registry → Discover → Authorize → Call   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       SYSTEM PATTERNS                            │
├─────────────────────────────────────────────────────────────────┤
│ 11. GOAL SETTING:      SMART → KPIs → Monitor → Achieve        │
│ 12. EXCEPTION:         Try → Error → Classify → Recover        │
│ 13. HUMAN-IN-LOOP:     AI → Decision Gate → Human → Learn      │
│ 14. RAG:               Index → Query → Retrieve → Generate     │
│ 15. A2A COMM:          Agent → Message Broker → Agent          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    OPTIMIZATION PATTERNS                         │
├─────────────────────────────────────────────────────────────────┤
│ 16. RESOURCE-AWARE:    Classify → Route Model → Monitor        │
│ 17. REASONING:         Problem → Method (CoT/ToT/SC) → Solve   │
│ 18. GUARDRAILS:        Input → Sanitize → Risk → Moderate      │
│ 19. EVALUATION:        Tests → Monitor → Detect → Optimize     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     STRATEGIC PATTERNS                           │
├─────────────────────────────────────────────────────────────────┤
│ 20. PRIORITIZATION:    Score Tasks → Rank → Execute → Reorder  │
│ 21. EXPLORATION:       Scout → Cluster → Deep Dive → Discover  │
└─────────────────────────────────────────────────────────────────┘
```

## How to Use This Skill

### Step 1: Identify the Problem Domain

Map the user's problem to one of the five categories:

| Category         | Covers                                                                        |
|------------------|-------------------------------------------------------------------------------|
| **Core**         | Basic building blocks — chaining, routing, parallelization, reflection, tools |
| **Advanced**     | Coordination & intelligence — planning, multi-agent, memory, learning, MCP    |
| **System**       | Reliability & integration — goals, exceptions, HITL, RAG, A2A                 |
| **Optimization** | Efficiency & safety — resource-aware, reasoning, guardrails, evaluation       |
| **Strategic**    | Decision-making — prioritization, exploration                                 |

### Step 2: Match to the Right Pattern

Read the pattern's reference file in `references/` for the specific pattern.
Each reference file contains:

- **When to Use** — conditions that signal this pattern is appropriate
- **Where It Fits** — concrete application areas
- **Pros** — benefits and advantages
- **Cons** — trade-offs and limitations
- **Real-World Examples** — 6 concrete examples with implementation details

### Step 3: Present Trade-offs

Every pattern has trade-offs. Always present both pros and cons so the user
can make an informed decision. Common cross-cutting concerns:

- **Latency** — most patterns add processing time (chaining, reflection, multi-agent)
- **Cost** — more steps = more API calls (reflection multiplies cost, parallelization multiplies simultaneously)
- **Complexity** — coordination overhead grows with the number of components
- **Reliability** — more moving parts means more failure modes

### Step 4: Suggest Combinations

Patterns rarely work in isolation. Common effective combinations:

| Combination                          | Purpose                                      |
|--------------------------------------|----------------------------------------------|
| **Prompt Chaining + Reflection**     | High-quality output with self-improvement    |
| **Routing + Tool Use**               | Direct requests to specialized tools         |
| **Planning + Multi-Agent**           | Coordinate complex multi-step projects       |
| **Memory + Learning**                | Persistent knowledge that improves over time |
| **RAG + Reflection**                 | Grounded responses with quality improvement  |
| **Guardrails + Evaluation**          | Safe operation with continuous monitoring    |
| **Parallelization + Resource-Aware** | Fast processing with cost optimization       |
| **Reasoning + Reflection**           | Deep analysis with iterative refinement      |
| **HITL + Exception Handling**        | Graceful degradation with human oversight    |
| **Goal Setting + Prioritization**    | Objective-driven task management             |

## Pattern Quick Reference

### Core Patterns

| # | Pattern                                                  | One-Liner                                 |
|---|----------------------------------------------------------|-------------------------------------------|
| 1 | [Prompt Chaining](references/core/01-prompt-chaining.md) | Break complex tasks into sequential steps |
| 2 | [Routing](references/core/02-routing.md)                 | Direct requests to the right handler      |
| 3 | [Parallelization](references/core/03-parallelization.md) | Run independent tasks simultaneously      |
| 4 | [Reflection](references/core/04-reflection.md)           | Self-evaluate and improve iteratively     |
| 5 | [Tool Use](references/core/05-tool-use.md)               | Integrate external capabilities           |

### Advanced Patterns

| #  | Pattern                                                                          | One-Liner                                  |
|----|----------------------------------------------------------------------------------|--------------------------------------------|
| 6  | [Planning](references/advanced/01-planning.md)                                   | Strategic task decomposition               |
| 7  | [Multi-Agent Collaboration](references/advanced/02-multi-agent-collaboration.md) | Coordinate multiple specialized agents     |
| 8  | [Memory Management](references/advanced/03-memory-management.md)                 | Store and retrieve context across sessions |
| 9  | [Learning and Adaptation](references/advanced/04-learning-and-adaptation.md)     | Improve performance over time              |
| 10 | [Model Context Protocol](references/advanced/05-model-context-protocol.md)       | Standardized agent communication           |

### System Patterns

| #  | Pattern                                                                                    | One-Liner                         |
|----|--------------------------------------------------------------------------------------------|-----------------------------------|
| 11 | [Goal Setting and Monitoring](references/system/01-goal-setting-and-monitoring.md)         | Track objectives with KPIs        |
| 12 | [Exception Handling and Recovery](references/system/02-exception-handling-and-recovery.md) | Graceful error management         |
| 13 | [Human-in-the-Loop](references/system/03-human-in-the-loop.md)                             | Incorporate human feedback        |
| 14 | [Knowledge Retrieval (RAG)](references/system/04-knowledge-retrieval-rag.md)               | Access external knowledge sources |
| 15 | [Inter-Agent Communication (A2A)](references/system/05-inter-agent-communication-a2a.md)   | Agent-to-agent messaging          |

### Optimization Patterns

| #  | Pattern                                                                                  | One-Liner                      |
|----|------------------------------------------------------------------------------------------|--------------------------------|
| 16 | [Resource-Aware Optimization](references/optimization/01-resource-aware-optimization.md) | Efficient resource usage       |
| 17 | [Reasoning Techniques](references/optimization/02-reasoning-techniques.md)               | Structured thinking approaches |
| 18 | [Guardrails/Safety](references/optimization/03-guardrails-safety-patterns.md)            | Ensure safe operations         |
| 19 | [Evaluation and Monitoring](references/optimization/04-evaluation-and-monitoring.md)     | Performance tracking           |

### Strategic Patterns

| #  | Pattern                                                                           | One-Liner              |
|----|-----------------------------------------------------------------------------------|------------------------|
| 20 | [Prioritization](references/strategic/01-prioritization.md)                       | Manage task importance |
| 21 | [Exploration and Discovery](references/strategic/02-exploration-and-discovery.md) | Find new solutions     |

## Implementation Checklist

When implementing any agentic pattern, verify:

- [ ] **Input validation** — sanitize and validate all inputs
- [ ] **Error handling** — graceful degradation on failure
- [ ] **Observability** — logging, metrics, and tracing in place
- [ ] **Cost awareness** — track API calls and token usage
- [ ] **Security** — least-privilege access, credential management
- [ ] **Testing** — test each pattern component independently
- [ ] **Fallbacks** — define fallback behavior for each pattern step
- [ ] **Rate limiting** — respect API quotas and rate limits

## Source Attribution

All patterns are sourced from
[promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs)
under the MIT License. The patterns are distilled from extensive research on
agentic AI systems, made accessible through simple visual representations
and clear explanations.
