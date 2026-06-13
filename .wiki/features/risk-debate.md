---
title: "Risk Debate"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/RiskDebateService.java"
  - "src/main/java/com/embabel/gekko/agent/RiskAssessment.java"
  - "src/main/java/com/embabel/gekko/agent/RiskLevel.java"
  - "src/main/resources/prompts/risk/"
  - "src/main/resources/prompts/managers/RiskManager.jinja"
updated_at: "2026-06-13"
---

# Risk Debate

The risk debate evaluates an investment plan from multiple risk perspectives using a dedicated `RiskDebateService`. It produces a `RiskAssessment` with a `RiskLevel` (RISKY, NEUTRAL, or CONSERVATIVE).

## The Three Risk Agents

| Agent | Stance |
|-------|--------|
| **Aggressive** | Argues for taking bigger positions, accepting higher risk |
| **Conservative** | Argues for caution, smaller positions, or avoiding the trade |
| **Neutral** | Provides an unbiased assessment of the risks |

## How It Works

1. `RiskDebateService.runMultiAgentRiskDebate()` runs 3 rounds of debate
2. In each round, all three debaters respond to each other's arguments
3. The Aggressive debater responds to Conservative and Neutral
4. The Conservative debater responds to Aggressive and Neutral
5. The Neutral debater responds to Aggressive and Conservative
6. After 3 rounds, `RiskManager.jinja` judges the debate output
7. The result is parsed into a `RiskAssessment(RiskLevel, reasoning)`

## Output

The risk debate produces:

| Field | Type | Description |
|-------|------|-------------|
| `level` | `RiskLevel` | RISKY, NEUTRAL, or CONSERVATIVE |
| `reasoning` | `String` | LLM's explanation, truncated to 200 chars |

## When It Runs

The risk debate runs after the investment debate and human approval, as part of the full research pipeline.

## Prompt Templates

| Agent | Prompt file |
|-------|-------------|
| Aggressive | `prompts/risk/AggressiveDebator.jinja` |
| Conservative | `prompts/risk/ConservativeDebator.jinja` |
| Neutral | `prompts/risk/NeutralDebator.jinja` |
| Manager | `prompts/managers/RiskManager.jinja` |
