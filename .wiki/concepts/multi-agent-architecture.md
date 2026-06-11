---
title: "Multi-Agent Architecture"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BullResearcher.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BearResearcher.java"
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
updated_at: "2026-06-11"
---

# Multi-Agent Architecture

Gekko is built on the principle that **specialized agents working together produce better results than a single general-purpose agent**.

## Agent Roles

Each agent in the system has a specific role, tools, and prompt:

| Role | Agent Class | Responsibility |
|------|------------|----------------|
| **Trader (orchestrator)** | `TraderAgent` | Coordinates the workflow, calls other agents |
| **Fundamentals Analyst** | (action in TraderAgent) | Analyzes financial statements |
| **Market Analyst** | (action in TraderAgent) | Analyzes price data and indicators |
| **News Analyst** | (action in TraderAgent) | Analyzes news articles and sentiment |
| **Social Media Analyst** | (action in TraderAgent) | Analyzes social sentiment |
| **Bull Researcher** | `BullResearcher` | Argues for the investment |
| **Bear Researcher** | `BearResearcher` | Argues against the investment |
| **Risk Manager** | (prompt-based) | Evaluates risk from multiple perspectives |

## How Agents Communicate

Agents communicate through a **shared blackboard** (state):

1. Each agent writes its output to the blackboard as a typed record
2. The orchestrator reads outputs from the blackboard
3. The orchestrator decides what to do next based on the outputs
4. Conditional routing determines which agent runs next

## Agent Configuration

`TraderAgentConfig` controls agent behavior through:

- **LLM selection** — which model each agent uses (`tickerLlm`, `writerLlm`)
- **Role/goal/backstory** — personality templates for researchers, outliners, writers
- **Concurrency** — `maxConcurrency` controls parallel agent execution
- **Output directory** — where results are saved

## LLM Roles

Two LLM roles are configured:

| Role | Purpose | Typical Model |
|------|---------|---------------|
| `CHEAPEST_ROLE` | Data collection, routine analysis | Qwen3.6-35B-A3B |
| `BEST_ROLE` | Decision-making, final plans | Qwen3.6-35B-A3B |

By default, both point to the same model. In production, you'd use a cheaper model for data collection and a more capable one for reasoning.

## Why This Architecture

1. **Separation of concerns** — each agent does one thing well
2. **Swappable components** — replace one analyst without affecting others
3. **Debate-based reasoning** — opposing viewpoints surface hidden risks
4. **Human oversight** — critical decisions require human approval
5. **Caching** — results are cached to reduce cost and improve speed
