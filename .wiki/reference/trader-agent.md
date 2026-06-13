---
title: "Trader Agent (Code Reference)"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/TraderAgent.java"
updated_at: "2026-06-11"
---

# Trader Agent (Code Reference)

## File

`src/main/java/com/embabel/gekko/agent/TraderAgent.java`

## Overview

The `TraderAgent` is the main orchestrator. It's a single class with ~20 `@Action` methods that handle the entire trading workflow.

## Key Actions (in execution order)

| Action | Method | Purpose |
|--------|--------|---------|
| 1 | `tickerFromForm()` | Validate and sanitize user input |
| 2 | `tickerFromUserInput()` | Extract ticker from free-text input |
| 3 | `generateResearchPlan()` | Generate a research plan (pre-execution) |
| 4 | `waitForPlanApproval()` | HITL checkpoint: approve plan |
| 5 | `executeFullResearch()` | Run the full pipeline |
| 6a | `generateFundamentalsReport()` | Collect financial data |
| 6b | `generateMarketReport()` | Collect market data |
| 6c | `generateNewsReport()` | Collect news data |
| 6d | `generateSocialMediaReport()` | Collect social sentiment |
| 7 | `prepareDebateBriefs()` | Distill reports into briefs |
| 8 | `debateInvestment()` | Run bull/bear debate |
| 9 | `waitForReview()` | HITL checkpoint: review debate |
| 10 | `researchManager()` | Generate final investment plan |

## Key Dependencies

| Dependency | Type | Purpose |
|-----------|------|---------|
| `marketDataTools` | `MarketDataTools` | LLM tools for stock data and indicators |
| `fundamentalDataTools` | `FundamentalDataTools` | LLM tools for financial data |
| `newsDataTools` | `NewsDataTools` | LLM tools for news data |
| `riskDebateService` | `RiskDebateService` | Risk assessment debate |
| `cache` | `FileCache` | Disk-based caching layer |
| `config` | `TraderAgentConfig` | Configuration (LLM options, etc.) |
| `bullAgent` | `BullResearcher` | Bull argument agent |
| `bearAgent` | `BearResearcher` | Bear argument agent |
| `templateRenderer` | `TemplateRenderer` | Jinja template rendering |

## Prompt Resources

| Resource | Location |
|----------|----------|
| `promptFundamentalsAnalyst` | `classpath:prompts/analysts/FundamentalsAnalyst.jinja` |
| `promptMarketAnalyst` | `classpath:prompts/analysts/MarketAnalyst.jinja` |
| `promptNewsAnalyst` | `classpath:prompts/analysts/NewsAnalyst.jinja` |
| `promptSocialMediaAnalyst` | `classpath:prompts/analysts/SocialMediaAnalyst.jinja` |

## Streaming Support

Several actions support both streaming and non-streaming LLM calls:
- `tickerFromUserInput()` — uses `StreamingPromptRunner` when available
- `generateFundamentalsReport()` — uses template-based streaming
- `researchManager()` — uses template-based streaming

## Caching Keys

| Key Pattern | What It Caches |
|-------------|---------------|
| `{ticker}_ticker` | Extracted ticker |
| `{ticker}_fundamentals` | Fundamentals report |
| `{ticker}_market` | Market report |
| `{ticker}_news` | News report |
| `{ticker}_social_media` | Social media report |
| `{ticker}_briefs` | Distilled debate briefs |
| `{ticker}_debate_{n}_{bull|bear}` | Each debate turn |
| `{ticker}_research_manager` | Final investment plan |
| `{ticker}_research_plan` | Pre-execution research plan |
