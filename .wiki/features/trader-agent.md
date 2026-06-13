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
| `generateMarketReport()` | Gets stock price + technical indicators via `MarketDataTools` |
| `generateNewsReport()` | Fetches news with sentiment scores |
| `generateSocialMediaReport()` | Gets social sentiment data |
| `prepareDebateBriefs()` | Distills 4 analyst reports into 4 briefs |
| `debateInvestment()` | Runs Bull→Bear debate (up to `maxDebateIterations`) |
| `waitForReview()` | HITL checkpoint before final plan |
| `researchManager()` | Generates final investment plan |
| `generateResearchPlan()` | Creates a research plan for user review |
| `waitForPlanApproval()` | HITL checkpoint before full execution |
| `executeFullResearch()` | Runs the complete pipeline after approval |
| `assessRisk()` | Runs risk debate via `RiskDebateService` |

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
| Fundamentals Analyst | `prompts/analysts/FundamentalsAnalyst.jinja` |
| Market Analyst | `prompts/analysts/MarketAnalyst.jinja` |
| News Analyst | `prompts/analysts/NewsAnalyst.jinja` |
| Social Media Analyst | `prompts/analysts/SocialMediaAnalyst.jinja` |
| Debate Distiller | `prompts/debate/Distiller.jinja` |
| Research Manager | `prompts/managers/ResearchManager.jinja` |
| Risk Manager | `prompts/managers/RiskManager.jinja` |
| Trader | `prompts/trader/Trader.jinja` |
| Aggressive Debator | `prompts/risk/AggressiveDebator.jinja` |
| Conservative Debator | `prompts/risk/ConservativeDebator.jinja` |
| Neutral Debator | `prompts/risk/NeutralDebator.jinja` |

## Sub-Agents

The TraderAgent injects and calls sub-agent classes:

- **`BullResearcher`** — Argues in favor of the investment
- **`BearResearcher`** — Argues against the investment
- **`RiskDebateService`** — Runs risk debate between Aggressive, Conservative, and Neutral debaters

## Dependencies

| Dependency | Role |
|------------|------|
| `MarketDataTools` | Provides stock data and technical indicators |
| `FundamentalDataTools` | Provides financial statement data |
| `NewsDataTools` | Provides news data |
| `RiskDebateService` | Runs the risk assessment debate |
| `TemplateRenderer` | Renders Jinja prompt templates |
