---
title: "Trading Workflow"
type: "flow"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/OrchestratorAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateLoopAgent.java"
  - "src/main/java/com/embabel/gekko/agent/RiskDebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/Trader.java"
  - "src/main/java/com/embabel/gekko/agent/managers/PortfolioManager.java"
  - "src/main/java/com/embabel/gekko/web/TradingHtmxController.java"
updated_at: "2026-07-06"
---

# Trading Workflow

This page describes the complete step-by-step flow from user input to final investment plan.

## Step-by-Step Flow

### 1. User Enters Ticker

The user navigates to the web UI and enters a stock ticker (e.g., "AAPL").

- **Controller:** `TradingHtmxController`
- **Action:** `OrchestratorAgent.tickerFromForm()` validates and sanitizes the input
- **Result:** A `Ticker` record with sanitized, uppercase ticker symbol

### 2. Identity Resolution

The system resolves the ticker to its real company identity.

- **Agent:** `InstrumentIdentityAgent`
- **Action:** `OrchestratorAgent.resolveIdentity()`
- **Data source:** Yahoo Finance via `YFinService`
- **Output:** `InstrumentContext` (company name, sector, industry, exchange)
- **Purpose:** Prevents LLM hallucination about company details

### 3. Research Plan Generation

The system generates a high-level research plan.

- **Action:** `OrchestratorAgent.generateResearchPlan()` calls the ResearchManager prompt
- **Output:** A `ResearchPlan` record with a summary of what will be done
- **Checkpoint:** `OrchestratorAgent.waitForPlanApproval()` — user reviews and approves (HITL)

### 4. Data Collection (Analyst Reports)

If the plan is approved, `DebateAgent` orchestrates four analyst reports:

| Order | Analyst | What it collects |
|-------|---------|-----------------|
| 1 | Fundamentals | Financial statements, ratios, company overview |
| 2 | Market | Stock price data, technical indicators |
| 3 | News | Recent news articles with sentiment |
| 4 | Social Media | Social sentiment and discussion |

Each report is cached individually via `FileCache`.

### 5. Brief Distillation

Each full analyst report is distilled into a concise brief using `Distiller.jinja`:

```
4 Full Reports → 4 Briefs (DebateBriefs)
```

### 6. Investment Debate (Bull vs Bear)

`DebateLoopAgent` runs the bull/bear debate with convergence detection:

```
Round 1: Bull argues → Bear responds
Round 2: Bull argues → Bear responds
... (until convergence or max iterations)
```

Each turn sees the full conversation history. The loop stops when:
- **Jaccard similarity** between consecutive bull responses exceeds `similarityThreshold` (default: 0.8)
- **Max iterations** reached (`maxDebateIterations`, default: 5)

### 7. Trader Proposal

The `Trader` agent translates the research plan into a concrete transaction proposal (Buy/Hold/Sell with entry price, stop-loss, position sizing).

### 8. Risk Assessment

`RiskDebateAgent` runs a 3-round structured risk debate:

- **Debators:** Aggressive, Conservative, Neutral (round-robin order)
- **Rounds:** 3 rounds, each debator responds to the others
- **Output:** `RiskAssessment(RiskLevel, reasoning)`
- **Risk levels:** RISKY, NEUTRAL, CONSERVATIVE

### 9. Portfolio Decision

`PortfolioManager` synthesizes the risk debate, research plan, and trader proposal into a final portfolio decision.

### 10. Human Review (HITL Checkpoint)

After the full pipeline, the process enters a WAITING state:

- **Action:** `DebateAgent.waitForReview()` returns `WaitFor.formSubmission(...)`
- **UI:** `waiting.html` shows the debate history
- **User actions:** Provide feedback, approve or reject
- **Record:** `InvestmentReviewFeedback(feedback, approved)`

### 11. Final Investment Plan

If approved, the ResearchManager generates the final plan:

- **Action:** `DebateAgent.researchManager()` with debate history + risk assessment + portfolio decision + user feedback
- **Output:** `InvestmentPlan` with the final recommendation
- **UI:** `plan.html` or `plan-review.html` displays the plan

### 12. Decision Memory

The final decision is stored in decision memory for future learning:

- **Agent:** `DecisionMemoryAgent`
- **Action:** `DebateAgent.storeFinalDecision()`
- **Data stored:** Rating (Buy/Sell/Hold/etc.), summary, thesis
- **Future use:** Past context is injected into future research plans

## Visual Summary

```
User Input → OrchestratorAgent
    ├─→ Identity Resolution
    ├─→ Research Plan → [HITL]
    └─→ DebateAgent (asSubProcess)
            ├─→ 4 Analyst Reports
            ├─→ Distill Briefs
            ├─→ DebateLoopAgent (bull/bear debate with convergence)
            ├─→ Trader (transaction proposal)
            ├─→ RiskDebateAgent (3-way risk debate)
            ├─→ PortfolioManager (final decision)
            ├─→ [HITL]
            ├─→ InvestmentPlan
            └─→ DecisionMemory (store)
```

## Conditional Gates

The workflow has several conditional gates:

1. **Report sufficiency** — if analysts can't produce sufficient reports, the workflow exits early
2. **Investment conviction** — if the debate doesn't show strong conviction, the workflow may reject
3. **Debate convergence** — the debate stops when positions stabilize (similarity threshold) or max iterations reached