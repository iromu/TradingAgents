---
title: "Investment Debate"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BullResearcher.java"
  - "src/main/java/com/embabel/gekko/agent/researchers/BearResearcher.java"
  - "src/main/resources/prompts/researchers/"
  - "src/main/resources/prompts/debate/Distiller.jinja"
updated_at: "2026-06-11"
---

# Investment Debate

After the four analysts produce their reports, the system distills them into briefs and runs a **bull vs. bear debate**.

## The Distillation Step

Each full analyst report is too long for efficient debate. The system uses `Distiller.jinja` to produce a concise brief from each report:

```
Fundamentals Report → Fundamentals Brief
Market Report       → Market Brief
News Report         → News Brief
Social Media Report → Social Media Brief
```

These four briefs are bundled into a `DebateBriefs` record and cached.

## The Debate Loop

1. **Bull Researcher** argues why the stock is a good buy, given the briefs
2. **Bear Researcher** argues why the stock is risky or overvalued
3. They alternate turns, each seeing the full conversation history
4. The loop runs for **2 rounds** (bull → bear → bull → bear), hardcoded in `debateInvestment()`

## State Tracking

The debate tracks:
- `history` — full conversation as a list of strings
- `bullHistory` — only bull arguments
- `bearHistory` — only bear arguments
- `count` — number of turns completed
- `briefs` — the original distilled briefs

All state is stored in `InvestmentDebateState`.

## After the Debate

The debate result goes to one of two places:
1. **HITL checkpoint** (`waitForReview`) — pauses for human review
2. **ResearchManager** — generates the final investment plan if approved

## Prompt Templates

| Agent | Prompt file |
|-------|------------|
| Bull Researcher | `prompts/researchers/BullResearcher.jinja` |
| Bear Researcher | `prompts/researchers/BearResearcher.jinja` |
| Distiller | `prompts/debate/Distiller.jinja` |
