---
title: "Multi-Agent Architecture"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/OrchestratorAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateLoopAgent.java"
  - "src/main/java/com/embabel/gekko/agent/RiskDebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/Trader.java"
  - "src/main/java/com/embabel/gekko/agent/managers/PortfolioManager.java"
  - "src/main/java/com/embabel/gekko/agent/checkpoint/CheckpointAgent.java"
  - "src/main/java/com/embabel/gekko/agent/identity/InstrumentIdentityAgent.java"
  - "src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryAgent.java"
updated_at: "2026-07-06"
---

# Multi-Agent Architecture

Gekko is built on the principle that **specialized agents working together produce better results than a single general-purpose agent**. The architecture uses Embabel's `asSubProcess` pattern with isolated blackboards.

## Agent Hierarchy

```
OrchestratorAgent (entry point)
    │
    ├── InstrumentIdentityAgent (resolve ticker → company identity)
    ├── DecisionMemoryAgent (past context generation)
    └── DebateAgent (via asSubProcess)
            │
            ├── [4 Analyst Reports — cached LLM calls]
            ├── DebateLoopAgent (via asSubProcess)
            │       ├── BullResearcher
            │       └── BearResearcher
            ├── Trader (transaction proposal)
            ├── RiskDebateAgent (via asSubProcess)
            │       ├── AggressiveDebator
            │       ├── ConservativeDebator
            │       └── NeutralDebator
            ├── PortfolioManager (final decision)
            └── DecisionMemoryAgent (store decision)
```

## Core Agents

| Agent | Package | Role |
|-------|---------|------|
| **OrchestratorAgent** | `agent` | Entry point: ticker input, identity resolution, research plan, HITL approval, delegates to DebateAgent |
| **DebateAgent** | `agent` | Workflow orchestrator: 4 analyst reports, debate briefs, debate loop, trader, risk debate, portfolio decision, HITL review, final plan |
| **DebateLoopAgent** | `agent` | Bull/bear iterative debate with convergence detection (Jaccard similarity) |
| **RiskDebateAgent** | `agent` | 3-round risk debate (aggressive → conservative → neutral) with judgment |
| **Trader** | `agent` | Translates research plan into concrete transaction proposal |
| **PortfolioManager** | `agent` | Synthesizes risk debate, research plan, and trader proposal into final decision |

## Supplementary Agents

| Agent | Package | Role |
|-------|---------|------|
| **InstrumentIdentityAgent** | `agent.identity` | Resolves ticker to real company identity (name, sector, industry, exchange) via Yahoo Finance |
| **DecisionMemoryAgent** | `agent.memory` | Learns from past trading outcomes — stores decisions, resolves with actual returns, generates past context |
| **CheckpointAgent** | `agent.checkpoint` | Crash recovery via blackboard snapshot persistence |

## How Agents Communicate

Agents communicate through Embabel's **shared blackboard** pattern:

1. **OrchestratorAgent** is the entry point — it accepts user input and generates a research plan
2. After HITL approval, it delegates to **DebateAgent** via `asSubProcess`
3. **DebateAgent** orchestrates the full workflow, calling sub-agents via `asSubProcess`
4. Each sub-process has an isolated blackboard (spawned copy)
5. Results flow back through the blackboard

### `asSubProcess` Pattern

```java
// DebateAgent delegates to DebateLoopAgent
return actionContext.asSubProcess(
    ResearchTypes.InvestmentDebateState.class,
    getDebateLoopAgent()
);
```

`asSubProcess` creates an isolated blackboard (spawned copy), giving the sub-agent its own process context. This prevents cross-agent blackboard pollution and allows each agent to have its own planner type, model, and temperature.

## Agent Configuration

`TraderAgentConfig` controls agent behavior through:

- **LLM selection** — which model each agent uses (`tickerLlm`, `writerLlm`)
- **Role/goal/backstory** — personality templates for researchers, outliners, writers
- **Concurrency** — `maxConcurrency` controls parallel agent execution
- **Debate settings** — `maxDebateIterations`, `similarityThreshold`
- **Provider config** — multi-provider LLM settings (Anthropic, Google, OpenAI)

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
6. **Memory** — learns from past decisions to improve future recommendations
7. **Crash recovery** — checkpoint system allows resuming after failures