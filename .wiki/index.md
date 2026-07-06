---
title: "Gekko Project Wiki"
type: "index"
status: "active"
language: "default"
last_commit: "b64da1dbec4478b87493718db7a537e9cd3b004f"
updated_at: "2026-07-06"
---

# Gekko: Multi-Agent Trading Research Platform

## What is this?

Gekko is a research-oriented framework that decomposes stock analysis and trade-decision workflows into **specialized AI agents** — Market Analyst, News Analyst, Fundamentals Analyst, Social Media Analyst, Bull Researcher, Bear Researcher, a Trader, and Risk Managers. Each agent is an independent LLM-powered worker. They coordinate through a shared blackboard (state) using the [Embabel agent framework](https://github.com/embabel/embabel-agent).

The goal is to experiment with **multi-agent LLM coordination** on market analysis and trade decision tasks.

## Get Started

1. **Run it:** `./mvnw spring-boot:run` (or `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`)
2. **Required:** Set `OPENAI_API_KEY` and `OPENAI_BASE_URL` (defaults to `http://spark.local:4000`)
3. **Expected output:** The app starts on port 8080. Navigate to `http://localhost:8080` to enter a stock ticker and watch the agents work.
4. **First files to read:** This `index.md`, then `[[project-overview]]`, then `[[multi-agent-architecture]]`
5. **Safe first change:** Tweak a prompt template under `src/main/resources/prompts/` and see the output change
6. **Dangerous change:** Modify `DebateLoopAgent.debate()` — the debate convergence logic is fragile

## Why does it exist?

Gekko demonstrates the power of the Embabel agent framework by building a realistic, multi-step decision pipeline. It shows how different agents with different roles, tools, and prompts can collaborate to produce a more reasoned investment recommendation than a single LLM call.

## What happens when I run it?

1. You enter a stock ticker (e.g., "AAPL")
2. **OrchestratorAgent** resolves the ticker's identity and generates a research plan
3. You approve the plan via a **human-in-the-loop checkpoint** (HITL)
4. **DebateAgent** orchestrates the full research workflow: analyst reports, debate, risk assessment, portfolio decision
5. **Analyst agents** collect data: market data, news, fundamentals, social sentiment
6. **Bull and Bear researchers** debate the investment case (with convergence detection)
7. **Risk debate** evaluates the proposal from aggressive, conservative, and neutral perspectives
8. **Portfolio Manager** synthesizes everything into a final decision
9. Results are stored in **decision memory** for future learning

See `[[trading-workflow]]` for the full step-by-step flow.

## Where is data saved?

- **API response cache:** `data/alphavantage/` (Alpha Vantage JSON responses)
- **LLM response cache:** `data/llm/cache/` (cached LLM outputs keyed by ticker + report type)
- **Decision memory:** JSON files storing past trading decisions and outcomes
- **Checkpoints:** Blackboard snapshots for crash recovery
- **Config:** `src/main/resources/application.yaml` and `application-local.yaml`
- **Prompts:** `src/main/resources/prompts/` (Jinja templates for each agent role)

## What are the important moving parts?

| Area | Page |
|------|------|
| Agent architecture | `[[multi-agent-architecture]]` |
| Orchestrator agent | `[[trader-agent]]` |
| Agent configuration | `[[agent-configuration]]` |
| Market data tools | `[[market-data-tools]]` |
| Custom indicators | `[[technical-indicators]]` |
| Data sources | `[[data-sources]]` |
| Investment debate | `[[investment-debate]]` |
| Risk debate | `[[risk-debate]]` |
| Human-in-the-loop | `[[human-in-the-loop]]` |
| Caching layer | `[[file-cache]]` |
| Test coverage | `[[test-coverage]]` |

## What should I avoid breaking?

- **Debate convergence** — `DebateLoopAgent` uses Jaccard similarity to detect convergence. Changing the similarity threshold without testing can cause premature exits or infinite loops. See `[[risks/debate-convergence]]`.
- **FileCache race conditions** — `FileCache.getOrCompute()` has per-key locking but no cross-process protection. See `[[risks/filecache-race]]`.
- **Alpha Vantage API key** — hardcoded in `application-local.yaml` (gitignored). Don't commit it.
- **Cache key bugs** — some methods may ignore query parameters in cache keys. See `[[risks/cache-key-bug]]`.
- **OrchestratorAgent lookup** — agent discovery uses name matching, not `isInstance`. See `[[risks/orchestrator-agent-not-found]]`.

## Where do I look first?

- **Agent code:** `src/main/java/com/embabel/gekko/agent/` (OrchestratorAgent, DebateAgent, etc.)
- **Config:** `src/main/java/com/embabel/gekko/config/TraderAgentConfig.java`
- **Data layer:** `src/main/java/com/embabel/gekko/dataflows/` (AlphaVantageService, YFinService, FredService, PolymarketService)
- **Market tools:** `src/main/java/com/embabel/gekko/tools/MarketDataTools.java`
- **HITL:** `src/main/java/com/embabel/gekko/htmx/HitlService.java`, `src/main/java/com/embabel/gekko/htmx/HitlAgenticEventListener.java`
- **Caching:** `src/main/java/com/embabel/gekko/util/FileCache.java`
- **Utilities:** `[[date-utils]]`, `[[indicator-mapper]]`
- **UI:** `src/main/resources/templates/` (Thymeleaf templates)
- **Prompts:** `src/main/resources/prompts/` (Jinja templates)

## Embabel Framework Documentation

Distilled wiki pages covering the Embabel agent framework the project is built on:

| Page | Content |
|------|---------|
| `[[embabel-agent-framework]]` | Core concepts: agents, tools, MCP, DICE, planning strategies, key APIs |
| `[[embabel-tools-subagents]]` | Tool instances, tool groups, subagents, agentic tools, decoration pattern |
| `[[embabel-planning-strategies]]` | GOAP, Utility AI, Hybrid, Supervisor planners in detail |
| `[[embabel-testing]]` | FakePromptRunner, EmbabelMockitoIntegrationTest, unit and integration testing |

Raw source docs are in `docs/embabel-agent-docs/` (from [docs.embabel.com](https://docs.embabel.com)).