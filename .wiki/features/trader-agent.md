---
title: "Orchestrator & Debate Agents"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/OrchestratorAgent.java"
  - "src/main/java/com/embabel/gekko/agent/DebateAgent.java"
updated_at: "2026-07-06"
---

# Orchestrator & Debate Agents

The `TraderAgent` monolith was decomposed into two primary orchestrators: **OrchestratorAgent** (entry point) and **DebateAgent** (workflow orchestrator).

## OrchestratorAgent

The entry point for the trading research pipeline. Handles user input, identity resolution, research plan generation, and delegates to DebateAgent.

### Key Actions

| Action | Description |
|--------|-------------|
| `tickerFromForm()` | Validates and sanitizes user ticker input |
| `resolveIdentity()` | Resolves ticker to real company identity via `InstrumentIdentityAgent` |
| `generateResearchPlan()` | Generates a research plan using the ResearchManager prompt |
| `waitForPlanApproval()` | HITL checkpoint — user reviews and approves the plan |
| `resolvePendingDecisions()` | Resolves past decisions for this ticker from decision memory |
| `generatePastContext()` | Generates past context from decision memory for prompt injection |
| `executeDebate()` | Delegates to DebateAgent via `asSubProcess` |

## DebateAgent

Orchestrates the full research workflow after plan approval. Calls sub-agents via `asSubProcess` for isolated blackboards.

### Key Actions

| Action | Description |
|--------|-------------|
| `generateFundamentalsReport()` | Pulls financial data via LLM with analyst tools |
| `generateMarketReport()` | Gets stock price + technical indicators |
| `generateNewsReport()` | Fetches news with sentiment scores |
| `generateSocialMediaReport()` | Gets social sentiment data |
| `prepareDebateBriefs()` | Distills 4 analyst reports into 4 briefs |
| `runDebate()` | Runs bull/bear debate via `DebateLoopAgent` sub-process |
| `runTrader()` | Produces trader proposal via `Trader` agent |
| `runRiskDebate()` | Runs 3-round risk debate via `RiskDebateAgent` sub-process |
| `runPortfolioManager()` | Produces final portfolio decision |
| `waitForReview()` | HITL checkpoint after debate completes |
| `researchManager()` | Generates final investment plan |
| `storeFinalDecision()` | Stores decision to memory for future learning |

### Input Sanitization

`sanitizeForPrompt()` protects against prompt injection:
- Strips Jinja syntax (`{{`, `}}`, `{%`, `%}`)
- Removes code fences
- Rejects oversized input (>10,000 chars)
- Truncates output (>1,000 chars)
- Uses pre-compiled regex patterns (ReDoS mitigation)

## Data Flow

```
User Input
    │
    ▼
OrchestratorAgent: Ticker → Identity → Research Plan → [HITL]
    │
    ▼
DebateAgent: [4 Analyst Reports] → Briefs → Debate → Trader → Risk → Portfolio → [HITL] → Plan
```

## Caching

Every action result is cached via `FileCache`:
- Cache keys are built from ticker + type (e.g., `AAPL_fundamentals`, `AAPL_market`)
- Results are saved as both JSON (for structured access) and Markdown (for readability)
- Cache directory: `data/llm/cache/`

## Prompts

The agents use Jinja templates for their LLM prompts:

| Prompt | File |
|--------|------|
| Fundamentals Analyst | `prompts/analysts/FundamentalsAnalyst.jinja` |
| Market Analyst | `prompts/analysts/MarketAnalyst.jinja` |
| News Analyst | `prompts/analysts/NewsAnalyst.jinja` |
| Social Media Analyst | `prompts/analysts/SocialMediaAnalyst.jinja` |
| Debate Distiller | `prompts/debate/Distiller.jinja` |
| Research Manager | `prompts/managers/ResearchManager.jinja` |
| Risk Manager | `prompts/managers/RiskManager.jinja` |
| Trader | `prompts/managers/Trader.jinja` |
| Portfolio Manager | `prompts/managers/PortfolioManager.jinja` |

## Dependencies

| Dependency | Role |
|------------|------|
| `FileCache` | Disk-based caching layer |
| `InstrumentIdentityAgent` | Resolves ticker to company identity |
| `DecisionMemoryAgent` | Past decision context and storage |
| `CheckpointAgent` | Crash recovery via blackboard snapshots |
| `DebateLoopAgent` | Bull/bear iterative debate |
| `RiskDebateAgent` | 3-round risk debate |
| `Trader` | Transaction proposal |
| `PortfolioManager` | Final portfolio decision |
| `TemplateRenderer` | Renders Jinja prompt templates |