---
title: "Trading Workflow"
type: "flow"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
  - "src/main/java/com/embabel/gekko/web/TradingHtmxController.java"
updated_at: "2026-06-11"
---

# Trading Workflow

This page describes the complete step-by-step flow from user input to final investment plan.

## Step-by-Step Flow

### 1. User Enters Ticker

The user navigates to the web UI and enters a stock ticker (e.g., "AAPL").

- **Controller:** `TradingHtmxController`
- **Action:** `tickerFromForm()` validates and sanitizes the input
- **Result:** A `Ticker` record with sanitized, uppercase ticker symbol

### 2. Research Plan Generation

The system generates a high-level research plan.

- **Action:** `generateResearchPlan()` calls the ResearchManager prompt
- **Output:** A `ResearchPlan` record with a summary of what will be done
- **Checkpoint:** `waitForPlanApproval()` — user reviews and approves (HITL)

### 3. Data Collection (Analyst Reports)

If the plan is approved, four analyst agents run in sequence:

| Order | Analyst | What it collects |
|-------|---------|-----------------|
| 1 | Fundamentals | Financial statements, ratios, company overview |
| 2 | Market | Stock price data, technical indicators |
| 3 | News | Recent news articles with sentiment |
| 4 | Social Media | Social sentiment and discussion |

Each report is cached individually.

### 4. Brief Distillation

Each full analyst report is distilled into a concise brief using `Distiller.jinja`:

```
4 Full Reports → 4 Briefs (DebateBriefs)
```

### 5. Investment Debate (Bull vs Bear)

The Bull and Bear researchers debate the investment case:

```
Round 1: Bull argues → Bear responds
Round 2: Bull argues → Bear responds
```

Each turn sees the full conversation history. The loop runs exactly 2 rounds (4 turns total).

### 6. Human Review (HITL Checkpoint)

After the debate, the process enters a WAITING state:

- **Action:** `waitForReview()` returns `WaitFor.formSubmission(...)`
- **UI:** `waiting.html` shows the debate history (bull turns / bear turns)
- **User actions:** Provide feedback, approve or reject
- **Record:** `InvestmentReviewFeedback(feedback, approved)`

### 7. Final Investment Plan

If approved, the ResearchManager generates the final plan:

- **Action:** `researchManager()` with debate history + user feedback
- **Output:** `InvestmentPlan` with the final recommendation
- **UI:** `plan.html` or `plan-review.html` displays the plan

### 8. Logging

The full state is logged as JSON for later analysis and reflection.

## Conditional Gates

The workflow has several conditional gates:

1. **Report sufficiency** — if analysts can't produce sufficient reports, the workflow exits early
2. **Investment conviction** — if the debate doesn't show strong conviction, the workflow may reject
3. **Risk review** — risk debate runs only if the system determines it's needed

## Visual Summary

```
User Input → Research Plan → [HITL] → 4 Analyst Reports
    → Distill Briefs → Bull/Bear Debate → [HITL] → Investment Plan
    → Log Results
```
