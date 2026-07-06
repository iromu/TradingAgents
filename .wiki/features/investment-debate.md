---
title: "Investment Debate"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/DebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateLoopAgent.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BullResearcher.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BearResearcher.java"
  - "src/main/resources/prompts/researchers/"
  - "src/main/resources/prompts/debate/Distiller.jinja"
updated_at: "2026-07-06"
---

# Investment Debate

After the four analysts produce their reports, the system distills them into briefs and runs a **bull vs. bear debate** via `DebateLoopAgent`.

## The Distillation Step

Each full analyst report is too long for efficient debate. `DebateAgent.prepareDebateBriefs()` uses `Distiller.jinja` to produce a concise brief from each report:

```
Fundamentals Report → Fundamentals Brief
Market Report       → Market Brief
News Report         → News Brief
Social Media Report → Social Media Brief
```

These four briefs are bundled into a `DebateBriefs` record and cached.

## The Debate Loop

`DebateLoopAgent` runs an iterative bull/bear debate using Embabel's `RepeatUntilBuilder`:

1. **Bull Researcher** argues why the stock is a good buy, given the briefs
2. **Bear Researcher** argues why the stock is risky or overvalued
3. They alternate turns, each seeing the full conversation history
4. The loop runs until **convergence** or **max iterations**

### Convergence Detection

The debate uses **Jaccard similarity** on bigrams to detect when positions have stabilized:

- Compares consecutive bull responses
- If similarity ≥ `similarityThreshold` (default: 0.8), the debate stops
- Falls back to `maxDebateIterations` (default: 5) as a safety net

This replaces the old fixed-iteration approach, saving tokens when the debate converges early.

## State Tracking

The debate tracks:

- `history` — full conversation as a list of strings
- `bullHistory` — only bull arguments
- `bearHistory` — only bear arguments
- `count` — number of turns completed
- `briefs` — the original distilled briefs
- `currentResponse` — the last response (bear's final argument)

All state is stored in `InvestmentDebateState`.

## After the Debate

The debate result flows to:
1. **Trader** — produces a transaction proposal
2. **RiskDebateAgent** — assesses risk
3. **PortfolioManager** — produces final decision
4. **HITL checkpoint** — human review
5. **ResearchManager** — generates final investment plan

## Prompt Templates

| Agent | Prompt file |
|-------|------------|
| Bull Researcher | `prompts/researchers/BullResearcher.jinja` |
| Bear Researcher | `prompts/researchers/BearResearcher.jinja` |
| Distiller | `prompts/debate/Distiller.jinja` |