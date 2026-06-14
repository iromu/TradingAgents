---
title: "Emabel Agent Framework"
type: "concept"
status: "active"
language: "default"
source_paths: ["docs/embabel-agent-docs/overview.md", "docs/embabel-agent-docs/architecture.md", "docs/embabel-agent-docs/core-types.md", "docs/embabel-agent-docs/goals-actions-conditions.md", "docs/embabel-agent-docs/agent-platform.md", "docs/embabel-agent-docs/agentprocess.md"]
updated_at: "2026-06-14"
---

# Embabel Agent Framework

## What is it?

Embabel is a JVM-based agent orchestration framework built on Spring Boot. The TradingAgents project uses Embabel 0.5.0-SNAPSHOT as its agent layer.

## Core Concepts

- **Agent** — A self-contained component bundling domain logic, AI capabilities, and tools. Exposes `@Action` methods (discrete steps) annotated with `@AchievesGoal`, `@Condition`, etc.
- **Tools** — Extend LLM capabilities by letting agents interact with the outside world (APIs, databases, files). Inspired by ReAct (Reason + Act). Methods annotated with `@LlmTool` or Spring AI's `@Tool`.
- **MCP (Model Context Protocol)** — Standardized way of hosting/sharing tools across models and runtimes. Embabel can both consume and publish MCP tools.
- **DICE (Domain Integrated Context Engineering)** — Grounds LLM inputs and outputs in typed domain objects, replacing untyped prompts with structured, business-aware models.
- **Goals** — High-level objectives annotated with `@AchievesGoal`.
- **Actions** — Discrete steps within an agent's workflow, annotated with `@Action`.
- **Conditions** — Boolean checks that gate actions, annotated with `@Condition`.

## Why an Agent Framework?

LLMs alone lack explainability, discoverability, flow management, composability, guardrails, and safe integration with sensitive systems (databases). Agent frameworks break complex tasks into smaller, manageable components, offering greater control and predictability.

## Embabel Differentiators

- **Sophisticated Planning** — GOAP, Utility AI, Hybrid, and Supervisor planners go beyond finite state machines, enabling the system to combine known steps in novel orders.
- **Strong Typing** — Everything is strongly typed: actions, goals, conditions, domain models. Full refactoring support.
- **Platform Abstraction** — Clean separation between programming model and platform internals.
- **LLM Mixing** — Easy to use multiple LLMs for different tasks (cost-effective, privacy-conscious).
- **Spring/JVM Integration** — Built on Spring Boot with DI, AOP, persistence, and transactions.
- **Designed for Testability** — Unit testing and end-to-end agent testing are first-class concerns.

## Planning Strategies

| Planner | Best For | Description |
|---------|----------|-------------|
| **GOAP** (default) | Business processes with defined outputs | Goal-oriented, deterministic planning. Plans a path from current state to goal using preconditions and effects. |
| **Utility** | Exploration and event-driven systems | Selects the highest-value available action at each step. Greedy decisions based on immediate value. |
| **Hybrid** | Reducer pipelines | Like Utility for action picking, but exits as soon as any registered goal is satisfied. |
| **Supervisor** | Flexible multi-step workflows | LLM-orchestrated composition. An LLM selects which actions to call based on type schemas and gathered artifacts. |

## LLM Provider Support

OpenAI, Anthropic, Google Gemini, DeepSeek, Mistral, LM Studio, Ollama, OCI, AWS Bedrock, and any OpenAI-compatible endpoint (like LiteLLM).

## How TradingAgents Uses It

1. **Agent annotations** — `@Agent`, `@Action`, `@AchievesGoal` in `TraderAgent.java` and related classes
2. **LLM setup** — `embabel-agent-starter-openai-custom` targeting LiteLLM at `http://spark.local:4000`
3. **Multi-agent decomposition** — OrchestratorAgent, DebateAgent, DebateLoopAgent using `asSubProcess` with isolated blackboards
4. **Prompt templates** — Agent behavior in `src/main/resources/prompts/*.jinja`
5. **Structured outputs** — DICE-pattern records (e.g., `InvestmentReviewFeedback`, `RiskAssessment`) for typed LLM outputs

## Key APIs

- **`PromptRunner`** — All LLM calls go through this interface. Methods: `createObject()`, `createObjectIfPossible()`, `generateText()`, `rendering()`.
- **`LlmOptions`** — Fluent API for LLM configuration: `withModel()`, `withRole()`, `withTemperature()`, `withTopP()`.
- **`AgentImage`** — For vision-capable LLMs: `AgentImage.fromFile()`, `AgentImage.create()`.
- **`Subagent`** — Agent handoffs as tools: `Subagent.ofClass(MyAgent.class).consuming(MyInput.class)`.
- **`AgentProcess`** — Created every time an agent runs, has a unique ID, manages blackboard state.

## Testing

- **`FakePromptRunner`** and **`FakeOperationContext`** — Mock LLM interactions while verifying prompts, hyperparameters, and business logic.
- **`EmbabelMockitoIntegrationTest`** — Base class for integration testing with convenient stubbing methods.
- Unit tests call `@Action` methods directly with test fixtures. Integration tests exercise complete agent workflows.

## Where to Look

- **Getting started:** `docs/embabel-agent-docs/getting-started/`
- **Reference:** `docs/embabel-agent-docs/reference/` (annotations, configuration, testing, planners)
- **Examples:** `docs/embabel-agent-docs/examples/` (WriteAndReviewAgent, StarNewsFinder, FactChecker)
- **Templates:** `docs/embabel-agent-docs/templates/pom.xml`, `docs/embabel-agent-docs/templates/application.yml`
