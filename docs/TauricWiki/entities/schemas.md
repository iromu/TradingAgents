---
title: "Pydantic Schemas"
type: "entity"
status: "active"
language: "default"
source_paths: [tradingagents/agents/schemas.py]
updated_at: "2026-06-14"
---

# Pydantic Schemas

Structured output schemas used by decision-making agents. Defined in `tradingagents/agents/schemas.py`.

## PortfolioRating

5-tier rating used by Research Manager and Portfolio Manager:

| Value | Description |
|---|---|
| `Buy` | Strong buy |
| `Overweight` | Above-normal buy |
| `Hold` | Neutral |
| `Underweight` | Below-normal sell pressure |
| `Sell` | Strong sell |

## TraderAction

3-tier transaction direction used by the Trader:

| Value | Description |
|---|---|
| `Buy` | Execute a buy |
| `Hold` | Do nothing |
| `Sell` | Execute a sell |

## ResearchPlan

Produced by the Research Manager (after bull/bear debate):

| Field | Type | Description |
|---|---|---|
| `recommendation` | `PortfolioRating` | Investment recommendation |
| `rationale` | `str` | Debate summary with conclusion |
| `strategic_actions` | `str` | Concrete steps for the trader |

## TraderProposal

Produced by the Trader:

| Field | Type | Description |
|---|---|---|
| `action` | `TraderAction` | Buy / Hold / Sell |
| `reasoning` | `str` | Case for the action |
| `entry_price` | `float \| None` | Optional entry price target |
| `stop_loss` | `float \| None` | Optional stop-loss price |
| `position_sizing` | `str \| None` | Optional sizing guidance |

## PortfolioDecision

Produced by the Portfolio Manager (final output):

| Field | Type | Description |
|---|---|---|
| `rating` | `PortfolioRating` | Final position rating |
| `executive_summary` | `str` | Action plan: entry, sizing, risk, horizon |
| `investment_thesis` | `str` | Detailed reasoning from evidence |
| `price_target` | `float \| None` | Optional target price |
| `time_horizon` | `str \| None` | Optional holding period |

## SentimentBand

6-tier sentiment direction used by the Sentiment Analyst:

| Value | Score range |
|---|---|
| `Bullish` | 6.5–10 |
| `Mildly Bullish` | 5.5–6.4 |
| `Neutral` | ~4.5–5.5 |
| `Mixed` | ~4.5–5.5 |
| `Mildly Bearish` | 3.5–4.4 |
| `Bearish` | 0–3.4 |

## SentimentReport

Produced by the Sentiment Analyst:

| Field | Type | Description |
|---|---|---|
| `overall_band` | `SentimentBand` | Overall sentiment direction |
| `overall_score` | `float` | 0–10 sentiment intensity |
| `confidence` | `Literal["low", "medium", "high"]` | Confidence level |
| `narrative` | `str` | Full sentiment report with evidence |

## Render helpers

Each schema has a `render_*()` function that converts the Pydantic instance back to the markdown shape the rest of the system expects (memory log, CLI display, saved reports).
