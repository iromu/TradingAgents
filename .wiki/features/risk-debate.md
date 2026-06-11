---
title: "Risk Debate"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/resources/prompts/risk/"
  - "src/main/resources/prompts/managers/RiskManager.jinja"
updated_at: "2026-06-11"
---

# Risk Debate

After the investment debate and human approval, the system may run a **risk debate** to evaluate the plan from multiple risk perspectives.

## The Three Risk Agents

| Agent | Stance |
|-------|--------|
| **Risk-Seeking** | Argues for taking bigger positions, accepting higher risk |
| **Risk-Averse** | Argues for caution, smaller positions, or avoiding the trade |
| **Neutral** | Provides an unbiased assessment of the risks |

## How It Works

1. The `RiskManager.jinja` prompt orchestrates the debate
2. Each agent gets a turn to present its perspective
3. A neutral judge synthesizes the three views
4. The output feeds into the final investment plan

## When It Runs

The risk debate is **conditional** — it only runs if the system determines risk review is required. This is controlled by a routing decision after the investment plan is generated.

## Prompt Templates

| Agent | Prompt file |
|-------|-------------|
| Risk-Seeking | `prompts/risk/AggresiveDebator.jinja` |
| Risk-Averse | `prompts/risk/ConservativeDebator.jinja` |
| Neutral | `prompts/risk/NeutralDebator.jinja` |
| Manager | `prompts/managers/RiskManager.jinja` |
