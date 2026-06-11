---
title: "Project Overview"
type: "overview"
status: "active"
language: "default"
source_paths:
  - "README.md"
  - "pom.xml"
  - "src/main/java/com/embabel/gekko/GekkoApplication.java"
updated_at: "2026-06-11"
---

# Project Overview

## What is Gekko?

Gekko is a **multi-agent trading research platform** built on the Embabel agent framework. It demonstrates how specialized AI agents — each with its own role, tools, and prompts — can collaborate to analyze stocks and produce investment recommendations.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5.13 |
| Agent Framework | Embabel 0.5.0-SNAPSHOT |
| LLM Integration | OpenAI-compatible (LiteLLM at `http://spark.local:4000`) |
| Data Sources | Alpha Vantage API, Yahoo Finance |
| Technical Analysis | TA4J library |
| UI | Thymeleaf + HTMX |
| Build | Maven (Java 25) |
| Observability | OpenTelemetry + Micrometer |

## Architecture at a Glance

```
User (Web/CLI)
    │
    ▼
┌─────────────────────────────────┐
│       TraderAgent (main)        │
│  ┌───────────┐  ┌───────────┐  │
│  │  Analysts  │  │  Researchers│ │
│  │ (4 agents) │  │ (Bull/Bear) │ │
│  └───────────┘  └───────────┘  │
│         ▼                       │
│  ┌───────────┐  ┌───────────┐  │
│  │  Trader   │  │ Risk Mgr  │  │
│  └───────────┘  └───────────┘  │
└─────────────────────────────────┘
    │           │           │
    ▼           ▼           ▼
  Data        Cache       UI
  Sources     (File)     (HTMX)
```

## Key Design Decisions

- **Role-based agents**: Each agent has a specific job (collect data, argue a position, make a decision)
- **Debate-based reasoning**: Bull and Bear agents argue back-and-forth to surface both sides
- **Human-in-the-loop**: Critical decision points pause for human review
- **File-based caching**: API responses and LLM outputs are cached to disk to reduce cost and latency
- **Prompt-driven behavior**: Agent behavior is controlled by Jinja templates under `prompts/`

## Entry Points

- **Web UI:** `http://localhost:8080` — enter a ticker, watch agents work
- **CLI:** `TickerShellCommands` — run from the Spring Boot CLI
- **A2A:** Agents are automatically exportable to the A2A protocol (see `docs/a2a.md`)
