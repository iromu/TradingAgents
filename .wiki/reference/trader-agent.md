---
title: "Trader Agent (Code Reference)"
type: "reference"
status: "stale"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
updated_at: "2026-06-11"
---

# Trader Agent (Code Reference)

> **Status: Stale** — `TraderAgent.java` was deleted and replaced by a multi-agent architecture. See `[[multi-agent-architecture]]` for the current design.

## Migration

The monolithic `TraderAgent` (~784 lines) was decomposed into 7 agents across 4 packages:

| Old (TraderAgent) | New |
|-------------------|-----|
| `tickerFromForm()` | `OrchestratorAgent.tickerFromForm()` |
| `generateResearchPlan()` | `OrchestratorAgent.generateResearchPlan()` |
| `waitForPlanApproval()` | `OrchestratorAgent.waitForPlanApproval()` |
| `executeFullResearch()` | `OrchestratorAgent.executeDebate()` |
| Analyst report actions | `DebateAgent.generate*Report()` |
| `prepareDebateBriefs()` | `DebateAgent.prepareDebateBriefs()` |
| `debateInvestment()` | `DebateLoopAgent.debate()` |
| `assessRisk()` | `RiskDebateAgent.assessRisk()` |
| `researchManager()` | `DebateAgent.researchManager()` |
| Trader proposal | `Trader.traderProposal()` |
| Portfolio decision | `PortfolioManager.portfolioDecision()` |

## Caching Keys (preserved)

| Key Pattern | What It Caches |
|-------------|---------------|
| `{ticker}_fundamentals` | Fundamentals report |
| `{ticker}_market` | Market report |
| `{ticker}_news` | News report |
| `{ticker}_social_media` | Social media report |
| `{ticker}_briefs` | Distilled debate briefs |
| `{ticker}_debate_{n}_bull` | Each debate turn (bull) |
| `{ticker}_debate_{n}_bear` | Each debate turn (bear) |
| `{ticker}_research_manager` | Final investment plan |
| `{ticker}_research_plan` | Pre-execution research plan |