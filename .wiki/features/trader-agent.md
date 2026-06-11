---
title: "Trader Agent"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
updated_at: "2026-06-11"
---

# Trader Agent

The `TraderAgent` is the **central agent** in Gekko. It's the only `@Agent`-annotated class and orchestrates the entire research workflow.

## What it does

The TraderAgent is a **workflow orchestrator** — it doesn't make decisions itself. Instead, it:

1. **Collects data** through analyst actions (fundamentals, market, news, social)
2. **Distills reports** into concise debate briefs
3. **Runs debates** between Bull and Bear researchers
4. **Pauses for human review** at critical decision points (HITL)
5. **Generates final plans** using the ResearchManager prompt

## Key Actions

| Action | Description |
|--------|-------------|
| `tickerFromForm()` | Validates and sanitizes user ticker input |
| `generateFundamentalsReport()` | Pulls financial statements via `FundamentalDataTools` |
| `generateMarketReport()` | Gets stock price + technical indicators |
| `generateNewsReport()` | Fetches news with sentiment scores |
| `generateSocialMediaReport()` | Gets social sentiment data |
| `prepareDebateBriefs()` | Distills 4 analyst reports into 4 briefs |
| `debateInvestment()` | Runs Bull→Bear debate (2 rounds) |
| `waitForReview()` | HITL checkpoint before final plan |
| `researchManager()` | Generates final investment plan |
| `generateResearchPlan()` | Creates a research plan for user review |
| `waitForPlanApproval()` | HITL checkpoint before full execution |
| `executeFullResearch()` | Runs the complete pipeline after approval |

## Data Flow

```
User Input
    │
    ▼
Ticker → [4 Analyst Reports]
    │
    ▼
DebateBriefs → [Bull/Bear Debate]
    │
    ▼
[Human Review]
    │
    ▼
InvestmentPlan
```

## Caching

Every action result is cached via `FileCache`:
- Cache keys are built from ticker + type (e.g., `AAPL_fundamentals`, `AAPL_market`)
- Results are saved as both JSON (for structured access) and Markdown (for readability)
- Cache directory: `data/llm/cache/`

## Prompts

The agent uses Jinja templates for its LLM prompts:

| Prompt | File |
|--------|------|
| Fundamentals Analyst | `prompts/analysts/FundamentalsAnalyst.txt` |
| Market Analyst | `prompts/analysts/MarketAnalyst.txt` |
| News Analyst | `prompts/analysts/NewsAnalyst.txt` |
| Social Media Analyst | `prompts/analysts/SocialMediaAnalyst.txt` |
| Debate Distiller | `prompts/debate/Distiller.jinja` |
| Research Manager | `prompts/managers/ResearchManager.jinja` |
| Trader | `prompts/trader/Trader.jinja` |

## Sub-Agents

The TraderAgent injects and calls two sub-agent classes:

- **`BullResearcher`** — Argues in favor of the investment
- **`BearResearcher`** — Argues against the investment

Both use the same `argue()` method with different role prompts.
