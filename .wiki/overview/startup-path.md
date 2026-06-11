---
title: "Startup Path"
type: "overview"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/GekkoApplication.java"
  - "src/main/resources/application.yaml"
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
updated_at: "2026-06-11"
---

# Startup Path

## How the app starts

1. `GekkoApplication.main()` launches a Spring Boot application
2. `@EnableAgents(loggingTheme = "gekko")` activates the Embabel agent platform
3. `TraderAgentConfig` loads LLM options and other configuration from `application.yaml`
4. The agent platform scans for `@Agent`-annotated classes (currently just `TraderAgent`)
5. HTTP servers start (WebMVC + HTMX controllers)

## Configuration Profiles

The app runs with profiles: `base,app,observability,local`

| Profile | Purpose |
|---------|---------|
| `base` | Default application configuration |
| `app` | App-specific config (`application-app.yaml`) |
| `observability` | OpenTelemetry tracing (`application-observability.yaml`) |
| `local` | Local dev config (`application-local.yaml`, gitignored) |

## Key Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `OPENAI_API_KEY` | `dummy` | API key for the LLM provider |
| `OPENAI_BASE_URL` | `http://spark.local:4000` | LiteLLM endpoint URL |
| `OPENAI_MODEL` | `Qwen3.6-35B-A3B` | Model name for both cheapest and best roles |

## What happens at startup

1. **Agent platform initializes** — scans for `@Agent` classes, registers them
2. **LLMs are configured** — cheapest and best roles both point to the same model by default
3. **FileCache directory is created** — if it doesn't exist
4. **HTTP routes are registered** — `/`, `/status/{processId}`, `/plan`, etc.
5. **Logging theme is set** — Gekko's custom color palette and event listener

## First files to read after startup

1. `[[project-overview]]` — what the project is
2. `[[trader-agent]]` — the main agent that orchestrates everything
3. `[[trading-workflow]]` — the full step-by-step flow
4. `[[human-in-the-loop]]` — how HITL checkpoints work
