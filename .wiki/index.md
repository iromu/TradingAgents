---
title: "Gekko Project Wiki"
type: "index"
status: "active"
language: "default"
last_commit: "8695eacc8bbd6683a9bf630232e52f1e3b54b168"
updated_at: "2026-06-11"
---

# Gekko: Multi-Agent Trading Research Platform

## What is this?

Gekko is a research-oriented framework that decomposes stock analysis and trade-decision workflows into **specialized AI agents** — Market Analyst, News Analyst, Fundamentals Analyst, Social Media Analyst, Bull Researcher, Bear Researcher, a Trader, and Risk Managers. Each agent is an independent LLM-powered worker. They coordinate through a shared blackboard (state) using the [Embabel agent framework](https://github.com/embabel/embabel-agent).

The goal is to experiment with **multi-agent LLM coordination** on market analysis and trade decision tasks.

## Get Started

1. **Run it:** `./mvnw spring-boot:run` (or `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`)
2. **Required:** Set `OPENAI_API_KEY` and `OPENAI_BASE_URL` (defaults to `http://spark.local:4000`)
3. **Expected output:** The app starts on port 8080. Navigate to `http://localhost:8080` to enter a stock ticker and watch the agents work.
4. **First files to read:** This `index.md`, then `[[project-overview]]`, then `[[trader-agent]]`
5. **Safe first change:** Tweak a prompt template under `src/main/resources/prompts/` and see the output change
6. **Dangerous change:** Modify `debateInvestment()` in `TraderAgent` — the debate loop logic is fragile

## Why does it exist?

Gekko demonstrates the power of the Embabel agent framework by building a realistic, multi-step decision pipeline. It shows how different agents with different roles, tools, and prompts can collaborate to produce a more reasoned investment recommendation than a single LLM call.

## What happens when I run it?

1. You enter a stock ticker (e.g., "AAPL")
2. **Analyst agents** collect data in parallel: market data, news, fundamentals, social sentiment
3. Their reports are **distilled into briefs** for the debate
4. **Bull and Bear researchers** debate the investment case (2 rounds)
5. You review the debate via a **human-in-the-loop checkpoint** (HITL)
6. If approved, a **Research Manager** generates the final investment plan
7. Results are logged as JSON and the UI shows the plan

See `[[trading-workflow]]` for the full step-by-step flow.

## Where is data saved?

- **API response cache:** `data/alphavantage/` (Alpha Vantage JSON responses)
- **LLM response cache:** `data/llm/cache/` (cached LLM outputs keyed by ticker + report type)
- **Config:** `src/main/resources/application.yaml` and `application-local.yaml`
- **Prompts:** `src/main/resources/prompts/` (Jinja templates for each agent role)

## What are the important moving parts?

| Area | Page |
|------|------|
| Main agent logic | `[[trader-agent]]` |
| Agent configuration | `[[agent-configuration]]` |
| Data sources | `[[data-sources]]` |
| Debate flow | `[[investment-debate]]` |
| Risk debate | `[[risk-debate]]` |
| Human-in-the-loop | `[[human-in-the-loop]]` |
| Caching layer | `[[file-cache]]` |
| Custom indicators | `[[technical-indicators]]` |

## What should I avoid breaking?

- **Debate loop convergence** — `debateInvestment()` uses a fixed `maxIterations(2)`, not a convergence check. Changing this without care can cause infinite loops or premature exits. See `[[risks/debate-convergence]]`.
- **FileCache race conditions** — `FileCache.getOrCompute()` has a known race condition on concurrent access. See `[[risks/filecache-race]]`.
- **Alpha Vantage API key** — hardcoded in `application-local.yaml` (gitignored). Don't commit it.
- **Cache key bugs** — `getNews()` in `AlphaVantageService` ignores date range params in the cache key. See `[[risks/cache-key-bug]]`.

## Where do I look first?

- **Agent code:** `src/main/java/com/embabel/gekko/agent/TraderAgent.java`
- **Config:** `src/main/java/com/embabel/gekko/config/TraderAgentConfig.java`
- **Data layer:** `src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java`, `src/main/java/com/embabel/gekko/dataflows/YFinService.java`
- **UI:** `src/main/resources/templates/` (Thymeleaf templates)
- **Prompts:** `src/main/resources/prompts/` (Jinja templates)
