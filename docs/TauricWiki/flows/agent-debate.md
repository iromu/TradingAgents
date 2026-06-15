---
title: "Agent Debate"
type: "flow"
status: "active"
language: "default"
source_paths: [tradingagents/graph/conditional_logic.py, tradingagents/agents/researchers/, tradingagents/agents/risk_mgmt/]
updated_at: "2026-06-14"
---

# Agent Debate

TradingAgents uses two debate loops: bull/bear research debate and risk management debate. Both are controlled by conditional routing in `graph/conditional_logic.py`.

## Bull/Bear Debate (Phase 2)

### Flow

```
Bull Researcher → Bear Researcher → Bull Researcher → ... → Research Manager
```

### Controls

- `should_continue_debate()` checks the conversation `count` against `2 * max_debate_rounds`
- Each turn increments the count
- When the limit is reached, routing goes to `Research Manager`

### State

Each researcher reads:
- All four analyst reports
- The full debate history (from `investment_debate_state`)
- The previous response from the opponent

### Synthesis

The **Research Manager** (using `deep_think_llm`) reads the full debate and produces a structured `ResearchPlan` with recommendation, rationale, and strategic actions.

## Risk Debate (Phase 4)

### Flow

```
Aggressive → Conservative → Neutral → Aggressive → ... → Portfolio Manager
```

### Controls

- `should_continue_risk_analysis()` checks the conversation `count` against `3 * max_risk_discuss_rounds`
- Each turn increments the count
- When the limit is reached, routing goes to `Portfolio Manager`

### State

Each debater reads:
- The trader's proposal
- The full risk debate history (from `risk_debate_state`)
- All analyst reports

### Final decision

The **Portfolio Manager** (using `deep_think_llm`) produces the final `PortfolioDecision` with a 5-tier rating, executive summary, investment thesis, price target, and time horizon.

## Conditional routing

`ConditionalLogic` class in `graph/conditional_logic.py` provides:

| Method | Controls |
|---|---|
| `should_continue_debate()` | Bull/bear debate loop |
| `should_continue_risk_analysis()` | Risk debate loop |
| `should_continue_*()` per analyst | Tool-calling loops per analyst |
